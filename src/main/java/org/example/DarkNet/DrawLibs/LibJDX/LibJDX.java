package org.example.DarkNet.DrawLibs.LibJDX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.example.DarkNet.DarkNet;
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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
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

    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Long, Integer> nodeIdToPos;
    private ProductItem selectedProduct;
    private StoreMode selectedStore;

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
                item.mode = StoreMode.KRAKEN;
            }
        });

        TextButton megaBtn = new TextButton("Mega", skin);
        megaBtn.setSize(buttonWidth, buttonHeight);
        megaBtn.setPosition(item.x + gap, item.y - buttonHeight);
        megaBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                item.mode = StoreMode.MEGA;
            }
        });

        stage.addActor(krakenBtn);
        stage.addActor(megaBtn);
        buttons.add(krakenBtn);
        buttons.add(megaBtn);
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

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
