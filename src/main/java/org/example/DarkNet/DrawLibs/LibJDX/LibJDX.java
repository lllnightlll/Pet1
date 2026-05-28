package org.example.DarkNet.DrawLibs.LibJDX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.example.DarkNet.DarkNet;
import org.example.DarkNet.Kraken;
import org.example.DarkNet.MEGA;
import org.example.DarkNet.Data.Edge;
import org.example.DarkNet.Data.Node;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class LibJDX extends ApplicationAdapter {
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

    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Long, Integer> nodeIdToPos;
    private ProductItem selectedProduct;
    private StoreMode selectedStore;
    private boolean mapDrawing = false;
    private float graphScale = 0.67f;
    private float graphOffsetX = 400f;
    private float graphOffsetY = 200f;
    private long selectedStartNode = -1;
    private long selectedEndNode = -1;

    private LibJDX(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        this.nodes = nodes;
        this.edges = edges;
        this.nodeIdToPos = nodeIdToPos;
    }

    public static void initLibJDX(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(Assets.WINDOW_TITLE);
        config.setWindowedMode((int) WORLD_WIDTH, (int) WORLD_HEIGHT);
        config.setResizable(false);
        config.setWindowPosition(100, 100);
        new Lwjgl3Application(new LibJDX(nodes, edges, nodeIdToPos), config);
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

        Label endLabel = new Label("End:", skin);
        endLabel.setWidth(80f);

        endNodeField = new TextField("", skin);
        endNodeField.setWidth(120f);
        endNodeField.setMessageText("Node ID");

        findButton = new TextButton("Build", skin);

        findButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    selectedStartNode = startNodeField.getText().equals("") ? Long.parseLong(startNodeField.getText()) : -1;
                    selectedEndNode = startNodeField.getText().equals("") ? Long.parseLong(endNodeField.getText()) : -1;
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
        List<List<Node>> zakladka_map = new ArrayList<>();
        zakladka_map = selectedStore == StoreMode.KRAKEN ? (new Kraken()).dijkstraPath(nodes, edges, nodeIdToPos, selectedStartNode, selectedEndNode) : (new MEGA()).dijkstraPath(nodes, edges, nodeIdToPos, selectedStartNode, selectedEndNode);
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
