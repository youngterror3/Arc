package arc.backend.teavm;

import arc.*;
import arc.struct.*;
import arc.func.*;
import arc.util.*;
import arc.graphics.*;
import arc.util.Log.*;
import org.teavm.jso.*;
import org.teavm.jso.browser.*;
import org.teavm.jso.dom.html.*;

public class TeaApplication implements Application{
    private TeaApplicationConfig config;
    private HTMLCanvasElement canvas;
    private TeaGraphics graphics;
    private TeaInput input;
    private int lastWidth = -1, lastHeight = 1;
    private String clipboard = "";

    private final TaskQueue queue = new TaskQueue();
    private final Seq<ApplicationListener> listeners = new Seq<>();

    public TeaApplication(ApplicationListener listener, TeaApplicationConfig config){
        this.listeners.add(listener);
        this.config = config;
    }

    public void start(){
        TeaFileLoader.loadFiles(new TeaFileLoader.TeaVMFilePreloadListener(){
            @Override
            public void error(){
            }

            @Override
            public void complete(){
                startArc();
            }
        });
    }

    private void startArc(){
        canvas = config.canvas;

        Log.logger = new TeaLogger();

        Core.app = this;
        Core.graphics = graphics = new TeaGraphics(canvas, config);
        Core.gl = graphics.getGL20();
        Core.gl20 = graphics.getGL20();
        Core.files = new TeaFiles();
        Core.net = new TeaNet();
        Core.audio = new TeaAudio();
        Core.input = input = new TeaInput(canvas);
        Core.settings = new TeaSettings();
        listen(ApplicationListener::init);
        listen(l -> l.resize(canvas.getWidth(), canvas.getHeight()));
        delayedStep();
    }

    private void delayedStep(){
        Window.requestAnimationFrame(d -> step());
    }

    private void step(){
        graphics.update();
        graphics.frameId++;

        input.preUpdate();

        queue.run();

        Window window = Window.current();

        if(window.getInnerWidth() != canvas.getWidth() || window.getInnerHeight() != canvas.getHeight()){
            canvas.setWidth(window.getInnerWidth());
            canvas.setHeight(window.getInnerHeight());
        }

        if(lastWidth != canvas.getWidth() || lastHeight != canvas.getHeight()){
            lastWidth = canvas.getWidth();
            lastHeight = canvas.getHeight();
            Gl.viewport(0, 0, lastWidth, lastHeight);
            listen(l -> l.resize(canvas.getWidth(), canvas.getHeight()));
        }

        defaultUpdate();
        listen(ApplicationListener::update);
        input.postUpdate();
        delayedStep();
    }

    private void listen(Cons<ApplicationListener> cons){
        for(ApplicationListener l : listeners){
            cons.get(l);
        }
    }

    @Override
    public boolean openURI(String URI){
        Window.current().open(URI, "_blank");
        return true;
    }

    @Override
    public Seq<ApplicationListener> getListeners(){
        return listeners;
    }

    @Override
    public String getClipboardText(){
        return clipboard;
    }

    @Override
    public void setClipboardText(String text){
        this.clipboard = text;
    }

    @Override
    public void post(Runnable runnable){
        queue.post(runnable);
    }

    @Override
    public ApplicationType getType(){
        return ApplicationType.web;
    }

    @Override
    public void exit(){
    }

    public static class TeaApplicationConfig{
        public HTMLCanvasElement canvas;
        public boolean antialiasEnabled = false;
        public boolean stencilEnabled = false;
        public boolean alphaEnabled = false;
        public boolean premultipliedAlpha = true;
        public boolean drawingBufferPreserved = false;
    }

    public static class TeaLogger implements LogHandler{

        @Override
        public void log(LogLevel level, String text){
            consoleLog("[" + level.name() + "]: " + text);
        }

        @JSBody(params = "message", script = "console.log(\"Arc: \" + message);")
        native public static void consoleLog(String message);
    }
}
