package com.tomris.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Tomris'in kalbi: nefes alan bir çekirdek, farklı hızlarda dönen ince halkalar ve
 * halkalar üzerinde yörüngede süzülen parçacıklardan oluşan çok katmanlı bir enerji küresi.
 * <p>
 * Tasarım kasıtlı olarak soyuttur; bilinen "repulsor / arc reactor" (Iron Man / JARVIS)
 * imgesinden ayrışması için katı, tek renkli bir daire yerine yumuşak, akışkan eğrilerden
 * ve camgöbeğinden mora geçen zarif bir gradyandan oluşur.
 * <p>
 * Küre {@link EnergyLevel} durumuna göre üç farklı ruh haliyle "nefes alır":
 * dinlenirken yavaşça büyüyüp küçülür, düşünürken halkalar hızlanır, konuşurken
 * çekirdek titreşimli bir parıltıyla yanıp söner.
 */
public class EnergyOrb extends Pane {

    /** Küre'nin o an temsil ettiği aktivite düzeyi. */
    public enum EnergyLevel {
        IDLE(1.0, 0.35),
        THINKING(2.1, 0.6),
        SPEAKING(2.8, 1.0);

        final double animationRate;
        final double glowLevel;

        EnergyLevel(double animationRate, double glowLevel) {
            this.animationRate = animationRate;
            this.glowLevel = glowLevel;
        }
    }

    // Camgöbeğinden mora uzanan imza paleti — daha zarif ve "kadınsı" bir his için.
    private static final Color CYAN = Color.web("#22D3EE");
    private static final Color CYAN_BRIGHT = Color.web("#8AF7FF");
    private static final Color INDIGO = Color.web("#818CF8");
    private static final Color VIOLET = Color.web("#C084FC");
    private static final Color VIOLET_DEEP = Color.web("#7C3AED");

    private final double radius;
    private final Glow coreGlow = new Glow(EnergyLevel.IDLE.glowLevel);
    private final List<Animation> rateControlledAnimations = new ArrayList<>();
    private Timeline breathing;
    private Timeline speakingFlicker;

    public EnergyOrb(double radius) {
        this.radius = radius;
        setPrefSize(radius * 2.3, radius * 2.3);
        // Pane varsayılan olarak sınırsız büyüyebilir; ebeveyn (StackPane + HBox büyümesi)
        // bunu tüm kalan genişliğe kadar gererse, sabit koordinatlarda çizilen şekiller
        // artık büyümüş kutunun SOL ÜST köşesine yakın kalır ve küre yanlışlıkla sola
        // kaymış görünür. Maksimum boyutu tercih edilen boyuta sabitleyip bunu önlüyoruz;
        // StackPane böylece küreyi kendi hücresinde hem yatayda hem dikeyde gerçekten ortalar.
        setMaxSize(radius * 2.3, radius * 2.3);
        setMouseTransparent(true);
        buildLayers();
        startAnimations();
    }

    /** İki renk arasında 0..1 oranında yumuşak geçiş yapar (camgöbeği -> mor). */
    private static Color gradientColor(double t) {
        if (t < 0.5) {
            return CYAN.interpolate(INDIGO, t / 0.5);
        }
        return INDIGO.interpolate(VIOLET, (t - 0.5) / 0.5);
    }

    private void buildLayers() {
        // Panonun kendisi 2.3r genişliğinde; küre görsel olarak merkezde, halo/parçacıklar
        // için etrafında ekstra boşluk kalsın diye ofset kullanılır.
        double cx = radius * 1.15;
        double cy = radius * 1.15;

        // Kürenin dışına, çevresindeki boşluğa doğru uzanan çok soluk teknik çizgiler:
        // küre böylece boş zeminde asılı durmak yerine arka planla görsel olarak bağlanır.
        getChildren().add(buildBackgroundConnectors(cx, cy));

        getChildren().add(buildOuterHalo(cx, cy));

        // Sabit, çok soluk eşmerkezli "derinlik" halkaları — dönmezler, sadece katman hissi katar.
        getChildren().add(buildStaticRing(cx, cy, radius * 1.08, 0.10));
        getChildren().add(buildStaticRing(cx, cy, radius * 0.38, 0.14));

        // İç içe, farklı hız ve yönlerde dönen, dıştan içe doğru belirginleşen yarı saydam halkalar.
        double[] ringFactors = {0.48, 0.64, 0.80, 0.97};
        double[] ringPeriods = {6, 10, 15, 21};
        double[] ringOpacities = {0.55, 0.45, 0.35, 0.25};
        for (int i = 0; i < ringFactors.length; i++) {
            boolean clockwise = i % 2 == 0;
            Color ringColor = gradientColor(i / (double) (ringFactors.length - 1));
            getChildren().add(buildRotatingRing(cx, cy, radius * ringFactors[i],
                    ringPeriods[i], clockwise, ringColor, ringOpacities[i]));
        }

        // İnce, devre şeması hissi veren teknik çizgi dokusu.
        getChildren().add(buildCircuitLines(cx, cy));

        getChildren().add(buildPetalGroup(cx, cy));

        // Bazı halkaların üzerinde yörüngede süzülen küçük parçacıklar.
        getChildren().add(buildOrbitingParticle(cx, cy, radius * 0.64, 10, true, 0, CYAN_BRIGHT, 2.6));
        getChildren().add(buildOrbitingParticle(cx, cy, radius * 0.64, 10, true, 150, VIOLET, 2.2));
        getChildren().add(buildOrbitingParticle(cx, cy, radius * 0.80, 15, false, 60, Color.WHITE, 2.0));
        getChildren().add(buildOrbitingParticle(cx, cy, radius * 0.97, 21, true, 210, VIOLET_DEEP, 2.8));
        getChildren().add(buildOrbitingParticle(cx, cy, radius * 0.97, 21, true, 300, CYAN, 2.2));

        // Çekirdeğin hemen dışında, teknik bir enstrüman kadranı gibi ince işaret çizgileri.
        getChildren().add(buildBezelTicks(cx, cy));

        getChildren().add(buildCore(cx, cy));

        // Çekirdeğin üzerinde, okunaklı küçük "T.O.M.R.I.S" imzası. Üstteki ayrı sayfa
        // başlığından bağımsızdır; bu yüzden nefes alma/titreşim animasyonlarına katılmaz,
        // her zaman net okunur kalır.
        getChildren().add(buildCoreSignature(cx, cy));
    }

    /**
     * Kürenin dışına, halonun biraz ötesine uzanan, çok düşük opaklıkta düz çizgi ve küçük
     * halka karışımından oluşan bir "devre" dokusu. Diğer katmanlarla birlikte çok yavaş döner.
     */
    private Group buildBackgroundConnectors(double cx, double cy) {
        Group connectors = new Group();
        int count = 14;

        for (int i = 0; i < count; i++) {
            double angle = (360.0 / count) * i + (i % 2 == 0 ? 6 : 0);
            double startRadius = radius * 1.14;
            double endRadius = radius * (1.30 + (i % 4) * 0.06);
            Color color = gradientColor(i / (double) (count - 1));

            Line line = new Line(cx, cy - startRadius, cx, cy - endRadius);
            line.setStroke(color.deriveColor(0, 1, 1, 0.07 + (i % 3) * 0.02));
            line.setStrokeWidth(0.8);
            if (i % 3 == 0) {
                line.getStrokeDashArray().addAll(2.0, 4.0);
            }
            Group lineSegment = new Group(line);
            lineSegment.getTransforms().add(new Rotate(angle, cx, cy));
            connectors.getChildren().add(lineSegment);

            if (i % 2 == 0) {
                Circle node = new Circle(cx, cy - endRadius, 2.0);
                node.setFill(null);
                node.setStroke(CYAN_BRIGHT.deriveColor(0, 1, 1, 0.16));
                node.setStrokeWidth(0.8);
                Group nodeSegment = new Group(node);
                nodeSegment.getTransforms().add(new Rotate(angle, cx, cy));
                connectors.getChildren().add(nodeSegment);
            }
        }

        RotateTransition rotate = new RotateTransition(Duration.seconds(90), connectors);
        rotate.setByAngle(360);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
        rateControlledAnimations.add(rotate);

        return connectors;
    }

    /**
     * Çekirdeğin tam merkezinde, ince harf aralıklı, okunaklı küçük bir "T.O.M.R.I.S" imzası.
     * Font ölçüleri, sahneye eklenmeden de senkron olarak hesaplanabildiği için genişlik/yükseklik
     * doğrudan {@code getLayoutBounds()} ile okunup metin tek seferde tam merkeze yerleştirilir.
     */
    private javafx.scene.text.Text buildCoreSignature(double cx, double cy) {
        javafx.scene.text.Text signature = new javafx.scene.text.Text("T.O.M.R.I.S");
        signature.setFont(javafx.scene.text.Font.font("Segoe UI Semibold", 13));
        signature.setFill(Color.web("#EAFEFF"));
        signature.setStroke(Color.web("#04141A", 0.45));
        signature.setStrokeWidth(0.6);
        signature.setMouseTransparent(true);

        javafx.geometry.Bounds bounds = signature.getLayoutBounds();
        signature.setX(cx - bounds.getWidth() / 2);
        signature.setY(cy + bounds.getHeight() / 4);

        return signature;
    }

    /** Dönmeyen, çok soluk bir eşmerkezli halka; sadece görsel derinlik katmanı ekler. */
    private Circle buildStaticRing(double cx, double cy, double ringRadius, double opacity) {
        Circle ring = new Circle(cx, cy, ringRadius);
        ring.setFill(null);
        ring.setStroke(INDIGO.deriveColor(0, 1, 1, opacity));
        ring.setStrokeWidth(1.0);
        return ring;
    }

    /**
     * Devre şeması hissi veren ince teknik çizgiler: merkezden dışa uzanan kısa çizgiler,
     * her birinin ucunda dik bir "pad" işareti. Bazıları kesikli, bazıları düz çizgidir.
     * Katmanın tamamı, diğer halkalarla birlikte çok yavaş döner.
     */
    private Group buildCircuitLines(double cx, double cy) {
        Group traces = new Group();
        int count = 10;

        for (int i = 0; i < count; i++) {
            double angle = (360.0 / count) * i;
            double innerRadius = radius * (0.55 + (i % 3) * 0.05);
            double outerRadius = radius * (0.86 + (i % 2) * 0.07);
            Color color = gradientColor(i / (double) (count - 1));
            boolean dashed = i % 2 == 0;

            Line spoke = new Line(cx, cy - innerRadius, cx, cy - outerRadius);
            spoke.setStroke(color.deriveColor(0, 1, 1, 0.4));
            spoke.setStrokeWidth(1.0);
            if (dashed) {
                spoke.getStrokeDashArray().addAll(2.0, 3.0);
            }

            Line pad = new Line(cx - 3, cy - outerRadius, cx + 3, cy - outerRadius);
            pad.setStroke(color.deriveColor(0, 1, 1, 0.55));
            pad.setStrokeWidth(1.2);

            Group trace = new Group(spoke, pad);
            trace.getTransforms().add(new Rotate(angle, cx, cy));
            traces.getChildren().add(trace);
        }

        RotateTransition rotate = new RotateTransition(Duration.seconds(70), traces);
        rotate.setByAngle(360);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
        rateControlledAnimations.add(rotate);

        return traces;
    }

    /**
     * Çekirdeğin hemen dışında, bir enstrüman kadranı gibi ince işaret çizgilerinden oluşan
     * bir çerçeve. "T O M R I S" yazısı bu kürenin içinde değil, ayrı bir üst başlıkta olduğu
     * için burada gerçek harf yerine teknik/gravür hissi veren küçük çentikler kullanılır.
     */
    private Group buildBezelTicks(double cx, double cy) {
        Group ticks = new Group();
        double bezelRadius = radius * 0.42;
        int tickCount = 40;

        for (int i = 0; i < tickCount; i++) {
            double angle = (360.0 / tickCount) * i;
            boolean major = i % 5 == 0;

            Line tick = new Line(cx, cy - bezelRadius, cx, cy - bezelRadius - (major ? 7 : 3));
            tick.setStroke(CYAN_BRIGHT.deriveColor(0, 1, 1, major ? 0.55 : 0.22));
            tick.setStrokeWidth(major ? 1.3 : 0.8);
            tick.getTransforms().add(new Rotate(angle, cx, cy));
            ticks.getChildren().add(tick);
        }

        return ticks;
    }

    /** Kürenin dışına yayılan yumuşak, bulanık ışık halesi (camgöbeği -> mor nebula). */
    private Circle buildOuterHalo(double cx, double cy) {
        Circle halo = new Circle(cx, cy, radius * 1.15);
        halo.setFill(new RadialGradient(0, 0, cx, cy, radius * 1.15, false, CycleMethod.NO_CYCLE,
                new Stop(0, CYAN.deriveColor(0, 1, 1, 0.30)),
                new Stop(0.55, INDIGO.deriveColor(0, 1, 1, 0.16)),
                new Stop(0.85, VIOLET.deriveColor(0, 1, 1, 0.08)),
                new Stop(1, Color.TRANSPARENT)));
        halo.setEffect(new GaussianBlur(20));
        return halo;
    }

    /** İnce, yarı saydam, kesikli çizgili dönen bir enerji halkası. */
    private Circle buildRotatingRing(double cx, double cy, double ringRadius,
                                      double periodSeconds, boolean clockwise, Color color, double opacity) {
        Circle ring = new Circle(cx, cy, ringRadius);
        ring.setFill(null);
        ring.setStroke(color.deriveColor(0, 1, 1, opacity));
        ring.setStrokeWidth(1.3);
        ring.getStrokeDashArray().addAll(3.0, 7.0);

        RotateTransition rotate = new RotateTransition(Duration.seconds(periodSeconds), ring);
        rotate.setByAngle(clockwise ? 360 : -360);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
        rateControlledAnimations.add(rotate);

        return ring;
    }

    /**
     * Bir halka üzerinde sabit yarıçapta dönen, parlak küçük bir enerji parçacığı.
     * Rotasyon, küre merkezini pivot alan açık bir {@link Rotate} dönüşümüyle yapılır;
     * böylece parçacık kendi etrafında değil, halka boyunca yörüngede döner.
     */
    private Circle buildOrbitingParticle(double cx, double cy, double orbitRadius, double periodSeconds,
                                          boolean clockwise, double startAngleDegrees, Color color, double dotRadius) {
        Circle dot = new Circle(cx + orbitRadius, cy, dotRadius);
        dot.setFill(color);
        dot.setEffect(new Glow(0.6));

        Rotate pivotRotate = new Rotate(startAngleDegrees, cx, cy);
        dot.getTransforms().add(pivotRotate);

        Timeline orbit = new Timeline(new KeyFrame(Duration.seconds(periodSeconds),
                new KeyValue(pivotRotate.angleProperty(),
                        startAngleDegrees + (clockwise ? 360 : -360), Interpolator.LINEAR)));
        orbit.setCycleCount(Animation.INDEFINITE);
        orbit.play();
        rateControlledAnimations.add(orbit);

        return dot;
    }

    /**
     * Katı bir daire yerine, merkez etrafında yavaşça dönen kavisli yaylardan oluşan
     * "taç yaprağı" deseni. Tomris'in imzası olan, yumuşak ve akışkan orijinal siluet.
     */
    private Group buildPetalGroup(double cx, double cy) {
        Group petals = new Group();
        int petalCount = 6;
        double petalRadius = radius * 0.88;

        for (int i = 0; i < petalCount; i++) {
            double startAngle = (360.0 / petalCount) * i;
            Arc petal = new Arc(cx, cy, petalRadius, petalRadius * 1.08, startAngle, 46);
            petal.setType(ArcType.OPEN);
            petal.setFill(null);
            petal.setStroke(gradientColor(i / (double) (petalCount - 1)).deriveColor(0, 1, 1, 0.7));
            petal.setStrokeWidth(2.2);
            petal.setStrokeLineCap(StrokeLineCap.ROUND);
            petals.getChildren().add(petal);
        }

        RotateTransition rotate = new RotateTransition(Duration.seconds(46), petals);
        rotate.setByAngle(360);
        rotate.setInterpolator(Interpolator.LINEAR);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.play();
        rateControlledAnimations.add(rotate);

        return petals;
    }

    /** Kürenin parlak, nefes alan / titreşen merkezi çekirdeği. */
    private Circle buildCore(double cx, double cy) {
        Circle core = new Circle(cx, cy, radius * 0.32);
        core.setFill(new RadialGradient(0, 0, cx - radius * 0.08, cy - radius * 0.08, radius * 0.4, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE),
                new Stop(0.32, CYAN_BRIGHT),
                new Stop(0.7, CYAN),
                new Stop(1, VIOLET)));

        Bloom bloom = new Bloom(0.25);
        bloom.setInput(coreGlow);
        core.setEffect(bloom);

        // IDLE / THINKING: yavaş, düzenli nefes alma (büyüyüp küçülme).
        breathing = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(core.scaleXProperty(), 0.92, Interpolator.EASE_BOTH),
                        new KeyValue(core.scaleYProperty(), 0.92, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(1.4),
                        new KeyValue(core.scaleXProperty(), 1.08, Interpolator.EASE_BOTH),
                        new KeyValue(core.scaleYProperty(), 1.08, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(2.8),
                        new KeyValue(core.scaleXProperty(), 0.92, Interpolator.EASE_BOTH),
                        new KeyValue(core.scaleYProperty(), 0.92, Interpolator.EASE_BOTH))
        );
        breathing.setCycleCount(Animation.INDEFINITE);
        breathing.play();
        rateControlledAnimations.add(breathing);

        // SPEAKING: hızlı, düzensiz titreşimli parıltı (yalnızca konuşurken çalışır).
        speakingFlicker = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(coreGlow.levelProperty(), 0.75, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(90), new KeyValue(coreGlow.levelProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(150), new KeyValue(coreGlow.levelProperty(), 0.85, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(230), new KeyValue(coreGlow.levelProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(320), new KeyValue(coreGlow.levelProperty(), 0.78, Interpolator.EASE_BOTH))
        );
        speakingFlicker.setCycleCount(Animation.INDEFINITE);

        return core;
    }

    private void startAnimations() {
        applyEnergyLevel(EnergyLevel.IDLE);
    }

    /**
     * Kürenin "ruh halini" değiştirir: dinlenme, düşünme (Claude'dan yanıt beklenirken)
     * veya konuşma (sesli okuma sırasında). Tüm animasyonların hızı, çekirdeğin
     * parlaklığı ve titreşim davranışı bu duruma göre güncellenir.
     */
    public void setEnergyLevel(EnergyLevel level) {
        applyEnergyLevel(level);
    }

    private void applyEnergyLevel(EnergyLevel level) {
        for (Animation animation : rateControlledAnimations) {
            animation.setRate(level.animationRate);
        }

        if (level == EnergyLevel.SPEAKING) {
            speakingFlicker.playFromStart();
        } else {
            speakingFlicker.stop();
            Timeline glowTransition = new Timeline(new KeyFrame(Duration.seconds(0.4),
                    new KeyValue(coreGlow.levelProperty(), level.glowLevel, Interpolator.EASE_BOTH)));
            glowTransition.play();
        }
    }
}
