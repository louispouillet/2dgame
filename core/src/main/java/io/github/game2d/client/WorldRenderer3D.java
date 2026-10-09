package io.github.game2d.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.game2d.model.arena.Arena;
import io.github.game2d.model.arena.TileType;
import io.github.game2d.protocol.BrawlerState;
import io.github.game2d.protocol.ProjectileState;
import io.github.game2d.protocol.WorldSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldRenderer3D implements GameView {
    private static final float FIELD_OF_VIEW = 28f;
    private static final float CAMERA_HEIGHT = 16.4f;
    private static final float CAMERA_BACK = 11.5f;
    private static final float CAMERA_FOLLOW_SPEED = 5f;
    private static final float CAMERA_MARGIN = ArenaModelFactory.OUTSIDE_MARGIN / 2f;

    private static final float MODEL_RADIUS = 0.5f;
    private static final float BRAWLER_MODEL_HEIGHT = 2f;
    private static final float PROJECTILE_HEIGHT = 0.5f;
    private static final Color PLAYER_COLOR = new Color(0.20f, 0.50f, 1f, 1f);
    private static final Color ENEMY_COLOR = new Color(0.90f, 0.25f, 0.25f, 1f);
    private static final long ATTRIBUTES = Usage.Position | Usage.Normal;
    private static final Plane GROUND = new Plane(Vector3.Y, 0f);
    private static final Plane AIM_PLANE = new Plane(Vector3.Y, new Vector3(0f, PROJECTILE_HEIGHT, 0f));
    private static final BlendingAttribute SEE_THROUGH = new BlendingAttribute(0.6f);

    private record Bush(int col, int row, ModelInstance instance) {
    }

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch = new ModelBatch();
    private final Environment environment = new Environment();
    private final Model arenaModel;
    private final ModelInstance arenaInstance;
    private final Model bushModel;
    private final List<Bush> bushes = new ArrayList<>();
    private final Model brawlerModel;
    private final Model projectileModel;
    private final Map<Integer, ModelInstance> brawlerInstances = new HashMap<>();
    private final List<ModelInstance> projectileInstances = new ArrayList<>();
    private final Vector3 cameraTarget = new Vector3();
    private final Vector3 playerPosition = new Vector3();
    private final Vector3 aimPoint = new Vector3();
    private final Vector3 headPoint = new Vector3();
    private final Vector3 sidePoint = new Vector3();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BrawlerHud hud = new BrawlerHud();
    private final Matrix4 screenProjection = new Matrix4();
    private boolean followingPlayer;
    private final float arenaWidth;
    private final float arenaDepth;
    private float minTargetX;
    private float maxTargetX;
    private float minTargetZ;
    private float maxTargetZ;

    public WorldRenderer3D(Arena arena) {
        camera = new PerspectiveCamera(FIELD_OF_VIEW, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 100f;
        arenaWidth = arena.getWidth();
        arenaDepth = arena.getHeight();
        cameraTarget.set(arenaWidth / 2f, 0f, -arenaDepth / 2f);
        updateCameraBounds();

        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -0.4f, -1f, -0.6f));

        arenaModel = ArenaModelFactory.create(arena);
        arenaInstance = new ModelInstance(arenaModel);
        bushModel = ArenaModelFactory.createBush();
        for (int row = 0; row < arena.getHeight(); row++) {
            for (int col = 0; col < arena.getWidth(); col++) {
                if (arena.getTile(col, row) == TileType.BUSH) {
                    ModelInstance instance = new ModelInstance(bushModel);
                    instance.transform.setToTranslation(col, 0f, -row);
                    bushes.add(new Bush(col, row, instance));
                }
            }
        }

        ModelBuilder builder = new ModelBuilder();
        brawlerModel = builder.createCapsule(MODEL_RADIUS, BRAWLER_MODEL_HEIGHT, 16,
            new Material(ColorAttribute.createDiffuse(ENEMY_COLOR)), ATTRIBUTES);
        projectileModel = builder.createSphere(2 * MODEL_RADIUS, 2 * MODEL_RADIUS, 2 * MODEL_RADIUS, 12, 12,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)), ATTRIBUTES);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        updateCameraBounds();
    }

    @Override
    public void render(WorldSnapshot snapshot, Arena arena, int playerId) {
        BrawlerState player = snapshot.findBrawler(playerId);
        followPlayer(player, Gdx.graphics.getDeltaTime());
        updateBushes(arena, player);
        updateBrawlers(snapshot, playerId);
        updateProjectiles(snapshot);

        HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        ScreenUtils.clear(0.35f, 0.6f, 0.85f, 1f, true);
        modelBatch.begin(camera);
        modelBatch.render(arenaInstance, environment);
        for (Bush bush : bushes) {
            modelBatch.render(bush.instance(), environment);
        }
        modelBatch.render(brawlerInstances.values(), environment);
        for (int i = 0; i < snapshot.projectiles().size(); i++) {
            modelBatch.render(projectileInstances.get(i), environment);
        }
        modelBatch.end();
        drawHud(snapshot);
    }

    private void drawHud(WorldSnapshot snapshot) {
        List<HudAnchor> anchors = new ArrayList<>();
        for (BrawlerState brawler : snapshot.brawlers()) {
            float top = BRAWLER_MODEL_HEIGHT * brawler.radius() / MODEL_RADIUS;
            headPoint.set(brawler.x(), top, -brawler.y());
            sidePoint.set(brawler.x() + 1f, top, -brawler.y());
            camera.project(headPoint, 0f, 0f, camera.viewportWidth, camera.viewportHeight);
            camera.project(sidePoint, 0f, 0f, camera.viewportWidth, camera.viewportHeight);
            float unit = Math.abs(sidePoint.x - headPoint.x);
            anchors.add(new HudAnchor(brawler, headPoint.x, headPoint.y, unit));
        }
        screenProjection.setToOrtho2D(0f, 0f, camera.viewportWidth, camera.viewportHeight);
        shapes.setProjectionMatrix(screenProjection);
        batch.setProjectionMatrix(screenProjection);
        hud.draw(shapes, batch, anchors);
    }

    private void followPlayer(BrawlerState player, float frameTime) {
        if (player == null) {
            return;
        }
        playerPosition.set(player.x(), 0f, -player.y());
        clampToBounds(playerPosition);
        if (followingPlayer) {
            cameraTarget.lerp(playerPosition, MathUtils.clamp(CAMERA_FOLLOW_SPEED * frameTime, 0f, 1f));
        } else {
            cameraTarget.set(playerPosition);
            followingPlayer = true;
        }
        placeCamera();
    }

    private void updateBushes(Arena arena, BrawlerState player) {
        for (Bush bush : bushes) {
            boolean seeThrough = player != null && arena.canSeeIntoBush(bush.col(), bush.row(), player.x(), player.y());
            for (Material material : bush.instance().materials) {
                if (seeThrough) {
                    material.set(SEE_THROUGH);
                } else {
                    material.remove(BlendingAttribute.Type);
                }
            }
        }
    }

    private void updateCameraBounds() {
        camera.position.set(0f, CAMERA_HEIGHT, CAMERA_BACK);
        camera.up.set(Vector3.Y);
        camera.lookAt(0f, 0f, 0f);
        camera.update();
        Vector3 topLeft = groundPoint(0f, 0f);
        Vector3 topRight = groundPoint(camera.viewportWidth, 0f);
        Vector3 bottomLeft = groundPoint(0f, camera.viewportHeight);
        Vector3 bottomRight = groundPoint(camera.viewportWidth, camera.viewportHeight);

        float farHalfWidth = Math.max(Math.abs(topLeft.x), Math.abs(topRight.x));
        float farZ = Math.min(topLeft.z, topRight.z);
        float nearZ = Math.max(bottomLeft.z, bottomRight.z);

        minTargetX = farHalfWidth - CAMERA_MARGIN;
        maxTargetX = arenaWidth - farHalfWidth + CAMERA_MARGIN;
        if (minTargetX > maxTargetX) {
            minTargetX = arenaWidth / 2f;
            maxTargetX = minTargetX;
        }
        minTargetZ = -arenaDepth - CAMERA_MARGIN - farZ;
        maxTargetZ = CAMERA_MARGIN - nearZ;
        if (minTargetZ > maxTargetZ) {
            minTargetZ = -arenaDepth / 2f - (farZ + nearZ) / 2f;
            maxTargetZ = minTargetZ;
        }
        clampToBounds(cameraTarget);
        placeCamera();
    }

    private Vector3 groundPoint(float screenX, float screenY) {
        Ray ray = camera.getPickRay(screenX, screenY, 0f, 0f, camera.viewportWidth, camera.viewportHeight);
        Vector3 point = new Vector3();
        if (!Intersector.intersectRayPlane(ray, GROUND, point)) {
            throw new IllegalStateException("The camera must look down at the ground");
        }
        return point;
    }

    private void clampToBounds(Vector3 target) {
        target.x = MathUtils.clamp(target.x, minTargetX, maxTargetX);
        target.z = MathUtils.clamp(target.z, minTargetZ, maxTargetZ);
    }

    private void placeCamera() {
        camera.position.set(cameraTarget.x, CAMERA_HEIGHT, cameraTarget.z + CAMERA_BACK);
        camera.up.set(Vector3.Y);
        camera.lookAt(cameraTarget);
        camera.update();
    }

    private void updateBrawlers(WorldSnapshot snapshot, int playerId) {
        for (BrawlerState brawler : snapshot.brawlers()) {
            ModelInstance instance = brawlerInstances.computeIfAbsent(brawler.id(), id -> new ModelInstance(brawlerModel));
            Color color = brawler.id() == playerId ? PLAYER_COLOR : ENEMY_COLOR;
            instance.materials.get(0).set(ColorAttribute.createDiffuse(color));
            float scale = brawler.radius() / MODEL_RADIUS;
            instance.transform.setToTranslation(brawler.x(), BRAWLER_MODEL_HEIGHT * scale / 2, -brawler.y())
                .scale(scale, scale, scale);
        }
        brawlerInstances.keySet().removeIf(id -> snapshot.brawlers().stream().noneMatch(b -> b.id() == id));
    }

    private void updateProjectiles(WorldSnapshot snapshot) {
        List<ProjectileState> projectiles = snapshot.projectiles();
        while (projectileInstances.size() < projectiles.size()) {
            projectileInstances.add(new ModelInstance(projectileModel));
        }
        for (int i = 0; i < projectiles.size(); i++) {
            ProjectileState projectile = projectiles.get(i);
            float scale = projectile.radius() / MODEL_RADIUS;
            projectileInstances.get(i).transform.setToTranslation(projectile.x(), PROJECTILE_HEIGHT, -projectile.y())
                .scale(scale, scale, scale);
        }
    }

    @Override
    public Vector2 screenToWorld(int screenX, int screenY) {
        Ray ray = camera.getPickRay(screenX, screenY, 0f, 0f, camera.viewportWidth, camera.viewportHeight);
        if (!Intersector.intersectRayPlane(ray, AIM_PLANE, aimPoint)) {
            aimPoint.set(cameraTarget);
        }
        return new Vector2(aimPoint.x, -aimPoint.z);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        arenaModel.dispose();
        bushModel.dispose();
        brawlerModel.dispose();
        projectileModel.dispose();
        shapes.dispose();
        batch.dispose();
        hud.dispose();
    }
}
