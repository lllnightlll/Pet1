package org.example.DarkNet.DrawLibs.LibJDX;

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
import com.badlogic.gdx.utils.ScreenUtils;

public class LibJDX extends ApplicationAdapter {
    private ShapeRenderer shape;
    private OrthographicCamera camera;
    private Texture background;
    private SpriteBatch batch;

    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Long, Integer> nodeIdToPos;

    private LibJDX(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        this.nodes = nodes;
        this.edges = edges;
        this.nodeIdToPos = nodeIdToPos;
    }

    public static void initLibJDX(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(Assets.WINDOW_TITLE);
        config.setWindowedMode(1920, 1080);
        config.setResizable(true);
        config.setWindowPosition(100, 100);
        new Lwjgl3Application(new LibJDX(nodes, edges, nodeIdToPos), config);
    }

    @Override
    public void create() {
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 1920, 1080);
        background = new Texture(Gdx.files.internal(Assets.BACKGROUND_PNG));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void dispose() {
        shape.dispose();
        background.dispose();
        batch.dispose();
    }
}
