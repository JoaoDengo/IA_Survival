package io.github.AISurvivors.view.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import io.github.AISurvivors.model.state.GameSettings;

public class ConfigScreen implements Screen {
    private static final Color BACKGROUND_COLOR = new Color(0.05f, 0.06f, 0.08f, 1f);

    private final Game game;
    private final Stage stage;
    private final Skin skin;
    private final TextField playerNameField;
    private final SelectBox<String> difficultySelect;
    private final CheckBox musicCheckBox;
    private final Slider volumeSlider;
    private final Label volumeValueLabel;
    private final Label statusLabel;

    private boolean startGameRequested;
    private boolean backToMenuRequested;

    public ConfigScreen(Game game) {
        this.game = game;
        stage = new Stage();
        skin = createSkin();

        GameSettings settings = GameSettings.load();
        playerNameField = new TextField(settings.playerName, skin);
        playerNameField.setMessageText("Nome do jogador");
        playerNameField.setMaxLength(18);

        difficultySelect = new SelectBox<>(skin);
        difficultySelect.setItems(GameSettings.DIFFICULTIES);
        difficultySelect.setSelected(settings.difficulty);

        musicCheckBox = new CheckBox(" Musica ligada", skin);
        musicCheckBox.setChecked(settings.musicEnabled);

        volumeSlider = new Slider(0f, 100f, 1f, false, skin);
        volumeSlider.setValue(settings.musicVolume);
        volumeValueLabel = new Label("", skin);
        statusLabel = new Label("Configuracao carregada. Altere os campos e clique em Salvar.", skin);
        statusLabel.setWrap(true);

        updateVolumeLabel();
        buildLayout();
        addListeners();
    }

    private Skin createSkin() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont(
            Gdx.files.internal("ui/dialog_font.fnt"),
            Gdx.files.internal("ui/dialog_font.png"),
            false
        );
        font.setUseIntegerPositions(false);
        font.getData().setScale(1.1f);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        skin.add("default-font", font);
        skin.add("white", whiteTexture);
        skin.add("panel", coloredDrawable(whiteTexture, 0.11f, 0.13f, 0.18f, 1f, 480f, 420f), Drawable.class);

        LabelStyle labelStyle = new LabelStyle(font, new Color(0.91f, 0.95f, 1f, 1f));
        skin.add("default", labelStyle);

        TextButtonStyle buttonStyle = new TextButtonStyle();
        buttonStyle.up = coloredDrawable(whiteTexture, 0.16f, 0.32f, 0.48f, 1f, 150f, 42f);
        buttonStyle.down = coloredDrawable(whiteTexture, 0.08f, 0.20f, 0.34f, 1f, 150f, 42f);
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = new Color(0.75f, 0.9f, 1f, 1f);
        skin.add("default", buttonStyle);

        TextFieldStyle textFieldStyle = new TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.messageFontColor = new Color(0.62f, 0.68f, 0.76f, 1f);
        textFieldStyle.background = coloredDrawable(whiteTexture, 0.07f, 0.09f, 0.13f, 1f, 260f, 40f);
        textFieldStyle.focusedBackground = coloredDrawable(whiteTexture, 0.10f, 0.13f, 0.20f, 1f, 260f, 40f);
        textFieldStyle.cursor = coloredDrawable(whiteTexture, 0.80f, 0.92f, 1f, 1f, 2f, 30f);
        textFieldStyle.selection = coloredDrawable(whiteTexture, 0.22f, 0.42f, 0.62f, 1f, 1f, 1f);
        skin.add("default", textFieldStyle);

        CheckBoxStyle checkBoxStyle = new CheckBoxStyle();
        checkBoxStyle.checkboxOff = coloredDrawable(whiteTexture, 0.05f, 0.07f, 0.10f, 1f, 22f, 22f);
        checkBoxStyle.checkboxOn = coloredDrawable(whiteTexture, 0.18f, 0.62f, 0.34f, 1f, 22f, 22f);
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        checkBoxStyle.downFontColor = new Color(0.75f, 0.9f, 1f, 1f);
        skin.add("default", checkBoxStyle);

        SliderStyle sliderStyle = new SliderStyle();
        sliderStyle.background = coloredDrawable(whiteTexture, 0.06f, 0.08f, 0.12f, 1f, 250f, 8f);
        sliderStyle.knobBefore = coloredDrawable(whiteTexture, 0.17f, 0.47f, 0.70f, 1f, 1f, 8f);
        sliderStyle.knob = coloredDrawable(whiteTexture, 0.80f, 0.92f, 1f, 1f, 18f, 28f);
        skin.add("default-horizontal", sliderStyle);

        ListStyle listStyle = new ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.WHITE;
        listStyle.fontColorUnselected = new Color(0.76f, 0.82f, 0.90f, 1f);
        listStyle.selection = coloredDrawable(whiteTexture, 0.16f, 0.32f, 0.48f, 1f, 240f, 34f);

        ScrollPaneStyle scrollStyle = new ScrollPaneStyle();
        scrollStyle.background = coloredDrawable(whiteTexture, 0.05f, 0.07f, 0.10f, 1f, 240f, 100f);

        SelectBoxStyle selectBoxStyle = new SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = coloredDrawable(whiteTexture, 0.07f, 0.09f, 0.13f, 1f, 260f, 40f);
        selectBoxStyle.backgroundOpen = coloredDrawable(whiteTexture, 0.10f, 0.13f, 0.20f, 1f, 260f, 40f);
        selectBoxStyle.scrollStyle = scrollStyle;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        return skin;
    }

    private Drawable coloredDrawable(
        Texture texture,
        float red,
        float green,
        float blue,
        float alpha,
        float minWidth,
        float minHeight
    ) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        drawable.setMinWidth(minWidth);
        drawable.setMinHeight(minHeight);
        return drawable.tint(new Color(red, green, blue, alpha));
    }

    private void buildLayout() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table form = new Table();
        form.setBackground(skin.getDrawable("panel"));
        form.pad(28f);
        form.defaults().pad(8f);

        Label titleLabel = new Label("Configuracao do AISurvivors", skin);
        titleLabel.setFontScale(1.35f);
        Label subtitleLabel = new Label("Formulario salvo com Preferences antes de entrar no jogo.", skin);
        subtitleLabel.setFontScale(0.9f);

        form.add(titleLabel).colspan(3).padBottom(4f).row();
        form.add(subtitleLabel).colspan(3).padBottom(18f).row();
        addFormRow(form, "Nome", playerNameField);
        addFormRow(form, "Dificuldade", difficultySelect);
        addFormRow(form, "Audio", musicCheckBox);

        form.add(new Label("Volume", skin)).left();
        form.add(volumeSlider).width(260f).height(36f).left();
        form.add(volumeValueLabel).width(58f).left().row();

        TextButton backButton = new TextButton("Voltar", skin);
        TextButton saveButton = new TextButton("Salvar", skin);
        TextButton playButton = new TextButton("Salvar e Jogar", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                backToMenuRequested = true;
            }
        });
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveSettings();
            }
        });
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveSettings();
                startGameRequested = true;
            }
        });

        form.add(backButton).colspan(1).width(150f).height(44f).padTop(18f);
        form.add(saveButton).colspan(1).width(150f).height(44f).padTop(18f);
        form.add(playButton).colspan(1).width(220f).height(44f).padTop(18f).row();
        form.add(statusLabel).colspan(3).width(500f).padTop(10f).row();

        root.add(form).width(600f).pad(24f);
    }

    private void addFormRow(Table form, String labelText, Actor field) {
        form.add(new Label(labelText, skin)).left();
        form.add(field).colspan(2).width(360f).height(42f).left().row();
    }

    private void addListeners() {
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateVolumeLabel();
            }
        });
    }

    private void updateVolumeLabel() {
        volumeValueLabel.setText((int) volumeSlider.getValue() + "%");
    }

    private void saveSettings() {
        GameSettings settings = new GameSettings();
        settings.playerName = playerNameField.getText();
        settings.difficulty = difficultySelect.getSelected();
        settings.musicEnabled = musicCheckBox.isChecked();
        settings.musicVolume = volumeSlider.getValue();
        settings.save();
        statusLabel.setText("Configuracao salva para " + settings.playerName + ".");
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, BACKGROUND_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        if (startGameRequested) {
            game.setScreen(new GameScreen(game));
            dispose();
        } else if (backToMenuRequested) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
