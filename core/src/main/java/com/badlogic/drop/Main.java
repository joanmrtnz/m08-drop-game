package com.badlogic.drop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {
    Texture backgroundTexture, restartTexture;
    Texture bucketTexture, dropTexture;
    Sound dropSound, gameOverSound;
    Music music;
    SpriteBatch spriteBatch;
    FitViewport viewport;
    Sprite bucketSprite, restartSprite;
    Vector2 touchPos;
    Array<Sprite> dropSprites;
    float dropTimer;
    Rectangle bucketRectangle, dropRectangle, restartRectangle;
    int score;
    BitmapFont font;
    boolean gameOver;

    @Override
    public void create() {
        backgroundTexture = new Texture("background.png");
        restartTexture = new Texture("restart.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture = new Texture("drop.png");
        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("gameover.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));

        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(64, 40);
        bucketSprite = new Sprite(bucketTexture);
        bucketSprite.setSize(6, 6);
        restartSprite = new Sprite(restartTexture);
        restartSprite.setSize(10, 10);
        restartRectangle = new Rectangle();

        touchPos = new Vector2();
        dropSprites = new Array<>();
        bucketRectangle = new Rectangle();
        dropRectangle = new Rectangle();
        font = new BitmapFont();
        font.getData().setScale(0.2f);
        font.setColor(Color.WHITE);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        restartGame();
    }

    private void restartGame() {
        score = 0;
        gameOver = false;
        dropSprites.clear();
        bucketSprite.setPosition(viewport.getWorldWidth() / 2 - bucketSprite.getWidth() / 2, 2);
        restartSprite.setPosition(viewport.getWorldWidth() - restartSprite.getWidth() - 2, 1);
        music.setLooping(true);
        music.setVolume(0.5f);
        music.play();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        input();
        if (!gameOver) {
            logic();
        }
        draw();
    }

    private void input() {
        if (gameOver) {
            if (Gdx.input.justTouched()) {
                touchPos.set(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(touchPos);
                restartRectangle.set(restartSprite.getX(), restartSprite.getY(), restartSprite.getWidth(), restartSprite.getHeight());

                if (restartRectangle.contains(touchPos)) {
                    restartGame();
                }
            }
            return;
        }

        float speed = 20f;
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            bucketSprite.translateX(speed * delta);
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            bucketSprite.translateX(-speed * delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            bucketSprite.setCenterX(touchPos.x);
        }
    }

    private void logic() {
        if (gameOver) return;

        float worldWidth = viewport.getWorldWidth();
        float bucketWidth = bucketSprite.getWidth();

        bucketSprite.setX(MathUtils.clamp(bucketSprite.getX(), 0, worldWidth - bucketWidth));

        float delta = Gdx.graphics.getDeltaTime();
        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketSprite.getHeight());

        for (int i = dropSprites.size - 1; i >= 0; i--) {
            Sprite dropSprite = dropSprites.get(i);
            dropSprite.translateY(-6f * delta);
            dropRectangle.set(dropSprite.getX(), dropSprite.getY(), dropSprite.getWidth(), dropSprite.getHeight());

            // Si la gota toca el suelo, termina el juego
            if (dropSprite.getY() < 0) {
                gameOver = true;
                gameOverSound.play();
                music.stop();
                return;
            }

            // Hit test solo desde la parte superior del cubo
            float bucketTopY = bucketRectangle.y + bucketRectangle.height;
            float dropBottomY = dropRectangle.y;

            if (dropBottomY <= bucketTopY && dropBottomY > bucketTopY - 1 && dropRectangle.overlaps(bucketRectangle)) {
                dropSprites.removeIndex(i);
                dropSound.play();
                score++;
            }
        }

        dropTimer += delta;
        if (dropTimer > 1f) {
            dropTimer = 0;
            createDroplet();
        }
    }



    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        if (!gameOver) {
            spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
            bucketSprite.draw(spriteBatch);

            for (Sprite dropSprite : dropSprites) {
                dropSprite.draw(spriteBatch);
            }

            // Muestra el puntaje en la parte superior izquierda
            font.draw(spriteBatch, "Puntuació: " + score, 1, worldHeight - 1);

            // Muestra los FPS en la parte superior derecha
            font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), worldWidth - 8, worldHeight - 1);
        } else {
            font.getData().setScale(0.3f);
            font.draw(spriteBatch, "GAME OVER", worldWidth / 3, worldHeight / 2);
            font.draw(spriteBatch, "Puntuació: " + score, worldWidth / 3, worldHeight / 2 - 5);

            // Dibuja el botón de reinicio en la parte inferior derecha
            restartSprite.draw(spriteBatch);
        }

        spriteBatch.end();
    }

    private void createDroplet() {
        if (gameOver) return;

        float worldWidth = viewport.getWorldWidth();
        Sprite dropSprite = new Sprite(dropTexture);
        dropSprite.setSize(6, 6);
        dropSprite.setX(MathUtils.random(0f, worldWidth - dropSprite.getWidth()));
        dropSprite.setY(viewport.getWorldHeight());
        dropSprites.add(dropSprite);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        restartTexture.dispose();
        bucketTexture.dispose();
        dropTexture.dispose();
        dropSound.dispose();
        gameOverSound.dispose();
        music.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
