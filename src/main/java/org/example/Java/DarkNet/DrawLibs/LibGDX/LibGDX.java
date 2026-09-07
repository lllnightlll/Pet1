package org.example.Java.DarkNet.DrawLibs.LibGDX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.example.Java.DarkNet.Kraken;
import org.example.Java.DarkNet.MEGA;
import org.example.Java.DarkNet.Data.Edge;
import org.example.Java.DarkNet.Data.Node;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class LibGDX extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final List<ProductItem> products = new ArrayList<>();
    private static final List<TextButton> buttons = new ArrayList<>();

    private ShapeRenderer shape;
    private OrthographicCamera camera;
    private Viewport viewport;
    private Texture background;
    private SpriteBatch batch;
    private Stage stage;
    private Skin skin;
    private TextField startNodeField;
    private TextField endNodeField;
    private TextButton findButton;
    private Table bottomTable;
    private InputMultiplexer inputMultiplexer;
    private MapInputProcessor mapInputProcessor;
    private TextField activeField = null;

    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Long, Integer> nodeIdToPos;
    private ProductItem selectedProduct;
    private StoreMode selectedStore;
    private boolean mapDrawing = false;
    private boolean pathDrawing = false;
    private float graphScale = 0.67f;
    private float graphOffsetX = 400f;
    private float graphOffsetY = 200f;
    private long selectedStartNode = -1;
    private long selectedEndNode = -1;
    private List<List<Node>> path1 = new ArrayList<>();
    private List<List<Node>> path2 = new ArrayList<>();
    private int currentPathIndex = 0;
    private int pathSegmentsPerFrame = 5;
    private boolean endStepDraw = false;

    private LibGDX(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        this.nodes = nodes;
        this.edges = edges;
        this.nodeIdToPos = nodeIdToPos;
    }

    public static void initLibGDX(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(Assets.WINDOW_TITLE);
        config.setWindowedMode((int) WORLD_WIDTH, (int) WORLD_HEIGHT);
        config.setResizable(false);
        config.setWindowPosition(100, 100);
        new Lwjgl3Application(new LibGDX(nodes, edges, nodeIdToPos), config);
    }

    class MapInputProcessor implements InputProcessor {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button != Buttons.LEFT)
                return false;

            if (activeField == null) {
                return false;
            }

            float worldX = screenX;
            float worldY = Gdx.graphics.getHeight() - screenY;

            Node nearest = findNearestNode(worldX, worldY);
            if (nearest != null) {
                activeField.setText(String.valueOf(nearest.getId()));
                stage.setKeyboardFocus(null);
            }

            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            return false;
        }
    }

    private Node findNearestNode(float sx, float sy) {
        Node nearest = null;
        float minDist = Float.MAX_VALUE;

        for (Node node : nodes) {
            float nx = tx(node.x);
            float ny = ty(node.y);
            float dx = nx - sx;
            float dy = ny - sy;
            float dist = dx * dx + dy * dy;
            if (dist < minDist) {
                minDist = dist;
                nearest = node;
            }
        }

        return nearest;
    }

    private void createProductButtons(ProductItem item) {
        float buttonWidth = 110f;
        float buttonHeight = 34f;
        float gap = 2f;

        TextButton krakenBtn = new TextButton("Kraken", skin);
        krakenBtn.setSize(buttonWidth, buttonHeight);
        krakenBtn.setPosition(item.x - buttonWidth, item.y - buttonHeight);
        krakenBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedProduct = item;
                selectedStore = StoreMode.KRAKEN;
                clearAllProductButtons();
            }
        });

        TextButton megaBtn = new TextButton("Mega", skin);
        megaBtn.setSize(buttonWidth, buttonHeight);
        megaBtn.setPosition(item.x + gap, item.y - buttonHeight);
        megaBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedProduct = item;
                selectedStore = StoreMode.MEGA;
                clearAllProductButtons();
            }
        });

        stage.addActor(krakenBtn);
        stage.addActor(megaBtn);
        buttons.add(krakenBtn);
        buttons.add(megaBtn);
    }

    private void createBottomUI() {
        bottomTable = new Table();
        bottomTable.setPosition(1060f, 760f);

        Label startLabel = new Label("Start:", skin);
        startLabel.setWidth(80f);

        startNodeField = new TextField("", skin);
        startNodeField.setWidth(120f);
        startNodeField.setMessageText("Node ID");
        startNodeField.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activeField = startNodeField;
                stage.setKeyboardFocus(startNodeField);
            }
        });

        Label endLabel = new Label("End:", skin);
        endLabel.setWidth(80f);

        endNodeField = new TextField("", skin);
        endNodeField.setWidth(120f);
        endNodeField.setMessageText("Node ID");
        endNodeField.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activeField = endNodeField;
                stage.setKeyboardFocus(endNodeField);
            }
        });

        findButton = new TextButton("Build", skin);

        findButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    selectedStartNode = Long.parseLong(startNodeField.getText());
                    selectedEndNode = Long.parseLong(endNodeField.getText());
                    findPath();
                } catch (NumberFormatException e) {
                    throw e;
                }
            }
        });

        bottomTable.add(startLabel);
        bottomTable.add(startNodeField);
        bottomTable.add(endLabel);
        bottomTable.add(endNodeField);
        bottomTable.add(findButton);

        stage.addActor(bottomTable);
    }

    private void initProducts() {
        products.add(new ProductItem("Item1", 450, 575));
        products.add(new ProductItem("Item2", 775, 575));
        products.add(new ProductItem("Item3", 1100, 575));
        products.add(new ProductItem("Item4", 460, 310));
        products.add(new ProductItem("Item5", 785, 310));
        products.add(new ProductItem("Item6", 1110, 310));

        for (ProductItem item : products) {
            createProductButtons(item);
        }
    }

    private void clearAllProductButtons() {
        stage.getRoot().clearChildren();
        background = new Texture(Gdx.files.internal(Assets.MAP_PNG));
        mapDrawing = !mapDrawing;
        createBottomUI();
    }

    private float tx(double x) {
        return graphOffsetX + ((float) x) * graphScale;
    }

    private float ty(double y) {
        return graphOffsetY + ((float) y) * graphScale;
    }

    private void findPath() {
        List<List<Node>> zakladka_map = selectedStore == StoreMode.KRAKEN
                ? (new Kraken()).dijkstraPath(nodes, edges, nodeIdToPos, selectedStartNode, selectedEndNode)
                : (new MEGA()).dijkstraPath(nodes, edges, nodeIdToPos, selectedStartNode, selectedEndNode);

        if (!zakladka_map.isEmpty()) {
            pathDrawing = true;
            path1 = new ArrayList<>();
            List<Node> zakladka = selectedStore == StoreMode.KRAKEN ? zakladka_map.get(0) : zakladka_map.get(1);

            if (zakladka != null && zakladka.size() > 1) {
                for (int i = 0; i < zakladka.size() - 1; i++) {
                    Node a = zakladka.get(i);
                    Node b = zakladka.get(i + 1);
                    List<Node> temp = new ArrayList<>();
                    temp.add(a);
                    temp.add(b);
                    path1.add(temp);
                }
            }
            path2 = new ArrayList<>();
            zakladka = zakladka_map.get(1);
            if (zakladka != null && zakladka.size() > 1) {
                for (int i = 0; i < zakladka.size() - 1; i++) {
                    Node a = zakladka.get(i);
                    Node b = zakladka.get(i + 1);
                    List<Node> temp = new ArrayList<>();
                    temp.add(a);
                    temp.add(b);
                    path2.add(temp);
                }
            }

        }
    }

    private void drawingMapPath(List<List<Node>> path) {
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(121f / 255f, 250f / 255f, 242f / 255f, 255f / 255f);

        int count = 0;
        while (currentPathIndex < path.size() && count < pathSegmentsPerFrame) {
            List<Node> segment = path.get(currentPathIndex);
            Node a = segment.get(0);
            Node b = segment.get(1);
            shape.rectLine(tx(a.x), ty(a.y), tx(b.x), ty(b.y), 3f);
            currentPathIndex++;
            count++;
        }

        if (endStepDraw) {
            pathSegmentsPerFrame = Integer.MAX_VALUE;
        } else {
            pathSegmentsPerFrame = (int) (Math.random() * 5);
        }

        shape.end();

        if (currentPathIndex >= path.size()) {
            currentPathIndex = 0;
            endStepDraw = true;
        }
    }

    @Override
    public void create() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        background = new Texture(Gdx.files.internal(Assets.BACKGROUND_PNG));

        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal(Assets.UI_SKIN_JSON));

        mapInputProcessor = new MapInputProcessor();
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(mapInputProcessor);
        Gdx.input.setInputProcessor(inputMultiplexer);

        initProducts();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        if (mapDrawing) {
            shape.setProjectionMatrix(camera.combined);

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(91f / 255f, 126f / 255f, 119f / 255f, 32f / 255f);
            for (Edge edge : edges) {
                shape.rectLine(tx(edge.ux), ty(edge.uy), tx(edge.vx), ty(edge.vy), 0f);
            }
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(104f / 255f, 124f / 255f, 124f / 255f, 1f);
            for (Node node : nodes) {
                shape.circle(tx(node.x), ty(node.y), 1f / graphScale);
            }
            shape.end();

            if (pathDrawing && !path1.isEmpty() && !path2.isEmpty()) {
                if (!endStepDraw) {
                    drawingMapPath(path1);
                } else {
                    drawingMapPath(path2);
                }
            }
        }

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        shape.dispose();
        background.dispose();
        batch.dispose();
    }
}
