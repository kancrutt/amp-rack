package com.kancrutt.gigginggear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalChainCanvasView extends View {

    public static class Node {
        public int id;
        public int pluginId;
        public String name;
        public float x;
        public float y;
        public float width = 150;
        public float height = 82;
        public boolean selected;
        public int categoryColor;
        public int categoryIconRes;

        public Node(int id, int pluginId, String name, float x, float y) {
            this.id = id;
            this.pluginId = pluginId;
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public boolean contains(float tx, float ty) {
            return tx >= x && tx <= x + width && ty >= y && ty <= y + height;
        }

        public boolean isInDeleteButton(float tx, float ty) {
            float bx = x + width - 30;
            float by = y + 3;
            return tx >= bx && tx <= bx + 27 && ty >= by && ty <= by + 27;
        }

        public int getOutputX() { return (int) (x + width); }
        public int getOutputY() { return (int) (y + height / 2); }
        public int getInputX()  { return (int) x; }
        public int getInputY()  { return (int) (y + height / 2); }
    }

    public static class Edge {
        public int fromNodeId;
        public int toNodeId;

        public Edge(int from, int to) {
            this.fromNodeId = from;
            this.toNodeId = to;
        }
    }

    public interface OnNodeSelectedListener {
        void onNodeSelected(Node node);
    }
    public interface OnChainChangedListener {
        void onChainChanged();
    }
    public interface OnDeleteNodeListener {
        void onDeleteNode(Node node);
    }
    public interface OnEdgeCreatedListener {
        void onEdgeCreated(int fromId, int toId);
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Integer, Node> nodeMap = new HashMap<>();
    private final Map<Integer, Bitmap> iconCache = new HashMap<>();
    private final Paint paintNodeBody   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNodeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNodeSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint paintText     = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintWire       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintInputDot   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintOutputDot  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintHeader     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDeleteBtn  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDeleteX    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTempWire   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnNodeSelectedListener listener;
    private OnChainChangedListener chainChangedListener;
    private OnDeleteNodeListener deleteNodeListener;
    private OnEdgeCreatedListener edgeCreatedListener;

    // Node drag state
    private Node dragNode;
    private float lastX, lastY;
    private int dragFromIndex = -1;

    // Port drag state (C2 port-to-port wiring)
    private boolean draggingPort = false;
    private boolean draggingOutput = false;
    private Node portSourceNode = null;
    private float dragCurrentX, dragCurrentY;

    private static final int NODE_BG     = Color.parseColor("#1E2024");
    private static final int NODE_BORDER = Color.parseColor("#3A3D45");
    private static final int NODE_SEL    = Color.parseColor("#FFAA00");
    private static final int HEADER_BG  = Color.parseColor("#2A2D34");
    private static final int TEXT_COLOR  = Color.parseColor("#E0E0E0");
    private static final int WIRE_COLOR  = Color.parseColor("#FFAA00");
    private static final int DOT_COLOR   = Color.parseColor("#FFAA00");
    private static final int PORT_RADIUS = 30; // px hit area for ports (fatter for touch)

    public SignalChainCanvasView(Context ctx) { super(ctx); init(); }
    public SignalChainCanvasView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public SignalChainCanvasView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs); init(); }

    private void init() {
        paintNodeBody.setColor(NODE_BG);
        paintNodeBorder.setColor(NODE_BORDER);
        paintNodeSelected.setColor(NODE_SEL);
        paintText.setColor(TEXT_COLOR);
        paintWire.setColor(WIRE_COLOR);
        paintInputDot.setColor(DOT_COLOR);
        paintOutputDot.setColor(DOT_COLOR);
        paintHeader.setColor(HEADER_BG);
        paintDeleteBtn.setColor(Color.parseColor("#3A3D45"));
        paintDeleteX.setColor(NODE_SEL);
        paintDeleteX.setStrokeWidth(2.5f);
        paintDeleteX.setStyle(Paint.Style.STROKE);
        paintTempWire.setColor(Color.parseColor("#FFAA00"));
        paintTempWire.setAntiAlias(true);
        paintTempWire.setStrokeWidth(4f);
        paintTempWire.setStyle(Paint.Style.STROKE);

        paintWire.setStyle(Paint.Style.STROKE);
        paintWire.setStrokeWidth(4f);
        paintInputDot.setStyle(Paint.Style.FILL);
        paintOutputDot.setStyle(Paint.Style.FILL);
        paintText.setTextSize(26f);
    }

    public void setOnNodeSelectedListener(OnNodeSelectedListener l) { this.listener = l; }
    public void setOnChainChangedListener(OnChainChangedListener l) { this.chainChangedListener = l; }
    public void setOnDeleteNodeListener(OnDeleteNodeListener l) { this.deleteNodeListener = l; }
    public void setOnEdgeCreatedListener(OnEdgeCreatedListener l) { this.edgeCreatedListener = l; }

    private int getCategoryColor(String pluginName) {
        if (pluginName == null) return NODE_BORDER;
        String n = pluginName.toLowerCase();
        if (isAmpBlock(pluginName)) return Color.parseColor("#E8543C");
        if (n.contains("cab") || n.contains("cabinet") || n.contains("ir") || n.contains("impulse"))
            return Color.parseColor("#B0B0B0");
        if (n.contains("reverb") || n.contains("spring") || n.contains("hall") || n.contains("plate"))
            return Color.parseColor("#B85CF2");
        if (n.contains("delay") || n.contains("echo") || n.contains("tape"))
            return Color.parseColor("#4CD97B");
        if (n.contains("modulation") || n.contains("chorus") || n.contains("phaser") || n.contains("flanger") || n.contains("tremolo"))
            return Color.parseColor("#3D8BF2");
        if (n.contains("distortion") || n.contains("overdrive") || n.contains("dist"))
            return Color.parseColor("#F2A03D");
        if (n.contains("comp") || n.contains("dynamics") || n.contains("limiter"))
            return Color.parseColor("#FFB74D");
        if (n.contains("eq") || n.contains("equalizer") || n.contains("parametric"))
            return Color.parseColor("#A5D6A7");
        if (n.contains("pitch") || n.contains("synth") || n.contains("harmonizer") || n.contains("octave"))
            return Color.parseColor("#8B5CF2");
        if (n.contains("wah") || n.contains("filter") || n.contains("auto-wah") || n.contains("envelope"))
            return Color.parseColor("#3DE8F2");
        if (n.contains("volume") || n.contains("pan") || n.contains("boost") || n.contains("level"))
            return Color.parseColor("#3DE8C1");
        if (n.contains("noise gate") || n.contains("gate"))
            return Color.parseColor("#FFE0B2");
        return NODE_BORDER;
    }

    private int getCategoryIconRes(String pluginName) {
        if (pluginName == null) return R.drawable.ic_cat_volume;
        String n = pluginName.toLowerCase();
        if (isAmpBlock(pluginName)) return R.drawable.ic_cat_amp;
        if (n.contains("cab") || n.contains("cabinet") || n.contains("ir") || n.contains("impulse"))
            return R.drawable.ic_cat_cabinet;
        if (n.contains("reverb") || n.contains("spring") || n.contains("hall") || n.contains("plate"))
            return R.drawable.ic_cat_reverb;
        if (n.contains("delay") || n.contains("echo") || n.contains("tape"))
            return R.drawable.ic_cat_delay;
        if (n.contains("modulation") || n.contains("chorus") || n.contains("phaser") || n.contains("flanger") || n.contains("tremolo"))
            return R.drawable.ic_cat_modulation;
        if (n.contains("distortion") || n.contains("overdrive") || n.contains("dist"))
            return R.drawable.ic_cat_distortion;
        if (n.contains("comp") || n.contains("dynamics") || n.contains("limiter"))
            return R.drawable.ic_cat_dynamics;
        if (n.contains("eq") || n.contains("equalizer") || n.contains("parametric"))
            return R.drawable.ic_cat_eq;
        if (n.contains("pitch") || n.contains("synth") || n.contains("harmonizer") || n.contains("octave"))
            return R.drawable.ic_cat_pitch;
        if (n.contains("wah") || n.contains("filter") || n.contains("auto-wah") || n.contains("envelope"))
            return R.drawable.ic_cat_wah;
        if (n.contains("volume") || n.contains("pan") || n.contains("boost") || n.contains("level"))
            return R.drawable.ic_cat_volume;
        if (n.contains("noise gate") || n.contains("gate"))
            return R.drawable.ic_cat_gate;
        return R.drawable.ic_cat_volume;
    }

    private boolean isAmpBlock(String pluginName) {
        if (pluginName == null) return false;
        String n = pluginName.toLowerCase();
        return n.contains("neural amp") || n.contains("aida") || n.equals("amp") || n.contains("amp head") || n.contains("amplifier");
    }

    private static final float MAX_NODE_WIDTH = 280f;
    private static final float MIN_NODE_WIDTH = 150f;

    private float measureNodeWidth(String name) {
        if (name == null) return MIN_NODE_WIDTH;
        float textW = paintText.measureText(name);
        // Same layout geometry as drawNode(): icon pad 6 + icon 22 + gap 6 + text + right pad 22
        float w = 6 + 22 + 6 + textW + 22;
        return Math.max(MIN_NODE_WIDTH, Math.min(w, MAX_NODE_WIDTH));
    }

    private Bitmap getCategoryIcon(String pluginName) {
        int resId = getCategoryIconRes(pluginName);
        Bitmap bmp = iconCache.get(resId);
        if (bmp == null) {
            try {
                Drawable d = ContextCompat.getDrawable(getContext(), resId);
                if (d != null) {
                    // Rasterize at 2x (44px) so the 2dp strokes stay crisp when drawn ~22px
                    bmp = Bitmap.createBitmap(44, 44, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    d.setBounds(0, 0, 44, 44);
                    d.draw(c);
                    iconCache.put(resId, bmp);
                }
            } catch (Exception e) {
                Log.w("SignalChainCanvas", "icon render fail for " + pluginName, e);
            }
        }
        return bmp;
    }

    public void loadFromActivePlugins() {
        nodes.clear();
        edges.clear();
        nodeMap.clear();
        iconCache.clear();

        int count = 0;
        try {
            count = AudioEngine.getActivePlugins();
        } catch (Exception e) {
            Log.e("SignalChainCanvas", "AudioEngine not ready", e);
            requestLayout();
            invalidate();
            return;
        }

        float xCursor = 24;
        float yCenter = getHeight() > 0 ? getHeight() / 2f : 70f;

        for (int i = 0; i < count; i++) {
            String name = null;
            try { name = AudioEngine.getActivePluginName(i); } catch (Exception e) { Log.w("SignalChainCanvas", "name fail i=" + i, e); }
            if (name == null) name = "Plugin " + i;

            Node n = new Node(i, AudioEngine.getActivePluginID(i), name, xCursor, yCenter - 40);
            n.categoryColor = getCategoryColor(name);
            n.categoryIconRes = getCategoryIconRes(name);
            n.width = measureNodeWidth(name);
            nodes.add(n);
            nodeMap.put(n.id, n);
            xCursor += n.width + 40;
        }

        edges.clear();
        for (int i = 0; i < nodes.size() - 1; i++) {
            edges.add(new Edge(nodes.get(i).id, nodes.get(i + 1).id));
        }

        requestLayout();
        invalidate();
    }

    public void setGraphEdges(int[] from, int[] to) {
        edges.clear();
        if (from != null && to != null && from.length == to.length) {
            for (int i = 0; i < from.length; i++) {
                edges.add(new Edge(from[i], to[i]));
            }
        }
        invalidate();
    }

    // ── Port drag-to-connect helpers ──
    private boolean isNear(Node n, float tx, float ty, boolean output) {
        float px = output ? n.getOutputX() : n.getInputX();
        float py = output ? n.getOutputY() : n.getInputY();
        float dx = tx - px;
        float dy = ty - py;
        return dx * dx + dy * dy < PORT_RADIUS * PORT_RADIUS;
    }

    private Node findNearestPort(float tx, float ty, boolean lookingForInput, Node exclude) {
        Node best = null;
        float bestDist = PORT_RADIUS * PORT_RADIUS * 4; // 2x hit radius
        for (Node n : nodes) {
            if (n == exclude) continue;
            float px = lookingForInput ? n.getInputX() : n.getOutputX();
            float py = lookingForInput ? n.getInputY() : n.getOutputY();
            float dx = tx - px;
            float dy = ty - py;
            float d = dx * dx + dy * dy;
            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }
        return best;
    }

    private Node hitTest(float tx, float ty) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).contains(tx, ty)) return nodes.get(i);
        }
        return null;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 1) Cek delete button node yang dipilih
                Node sel = getSelectedNode();
                if (sel != null && sel.isInDeleteButton(tx, ty)) {
                    if (deleteNodeListener != null) deleteNodeListener.onDeleteNode(sel);
                    return true;
                }
                // 2) Cek port hit (output port dulu, baru input port)
                for (Node n : nodes) {
                    if (isNear(n, tx, ty, true)) {
                        draggingPort = true;
                        draggingOutput = true;
                        portSourceNode = n;
                        dragCurrentX = tx;
                        dragCurrentY = ty;
                        invalidate();
                        return true;
                    }
                    if (isNear(n, tx, ty, false)) {
                        draggingPort = true;
                        draggingOutput = false;
                        portSourceNode = n;
                        dragCurrentX = tx;
                        dragCurrentY = ty;
                        invalidate();
                        return true;
                    }
                }
                // 3) Node hit test (drag/select blok)
                dragNode = hitTest(tx, ty);
                if (dragNode != null) {
                    for (Node n : nodes) n.selected = false;
                    dragNode.selected = true;
                    lastX = tx;
                    lastY = ty;
                    dragFromIndex = dragNode.id;
                    if (listener != null) listener.onNodeSelected(dragNode);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (draggingPort) {
                    dragCurrentX = tx;
                    dragCurrentY = ty;
                    invalidate();
                    return true;
                }
                if (dragNode != null) {
                    float dx = tx - lastX;
                    float dy = ty - lastY;
                    dragNode.x += dx;
                    dragNode.y += dy;
                    lastX = tx;
                    lastY = ty;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingPort && portSourceNode != null) {
                    // Cari target port terdekat
                    Node target = findNearestPort(dragCurrentX, dragCurrentY, draggingOutput, portSourceNode);
                    if (target != null) {
                        Edge newEdge = draggingOutput
                                ? new Edge(portSourceNode.id, target.id)
                                : new Edge(target.id, portSourceNode.id);
                        //hindari duplikat
                        boolean exists = false;
                        for (Edge e : edges) {
                            if (e.fromNodeId == newEdge.fromNodeId && e.toNodeId == newEdge.toNodeId) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            edges.add(newEdge);
                            if (edgeCreatedListener != null) {
                                edgeCreatedListener.onEdgeCreated(newEdge.fromNodeId, newEdge.toNodeId);
                            }
                        }
                    }
                    draggingPort = false;
                    portSourceNode = null;
                    invalidate();
                    return true;
                }
                if (dragNode != null && dragFromIndex >= 0 && nodes.size() > 1) {
                    List<Node> sorted = new ArrayList<>(nodes);
                    Collections.sort(sorted, (a, b) -> Float.compare(a.x, b.x));
                    int newIndex = -1;
                    for (int i = 0; i < sorted.size(); i++) {
                        if (sorted.get(i).id == dragNode.id) {
                            newIndex = i;
                            break;
                        }
                    }
                    if (newIndex >= 0 && newIndex != dragFromIndex) {
                        try {
                            AudioEngine.movePlugin(dragFromIndex, newIndex);
                            postDelayed(() -> {
                                loadFromActivePlugins();
                                if (chainChangedListener != null) chainChangedListener.onChainChanged();
                            }, 80);
                        } catch (Exception e) {
                            Log.e("SignalChainCanvas", "movePlugin failed: from=" + dragFromIndex + " to=" + newIndex, e);
                        }
                    }
                }
                dragNode = null;
                dragFromIndex = -1;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private Node getSelectedNode() {
        for (Node n : nodes) if (n.selected) return n;
        return null;
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1) Fixed edges (kabel bezier antar node)
        for (Edge e : edges) {
            Node a = nodeMap.get(e.fromNodeId);
            Node b = nodeMap.get(e.toNodeId);
            if (a == null || b == null) continue;
            paintWire.setColor(a.categoryColor);
            drawBezierWire(canvas, a.getOutputX(), a.getOutputY(), b.getInputX(), b.getInputY());
        }

        // 2) Temporary wire saat port drag
        if (draggingPort && portSourceNode != null) {
            paintTempWire.setColor(portSourceNode.categoryColor);
            float sx = draggingOutput ? portSourceNode.getOutputX() : portSourceNode.getInputX();
            float sy = draggingOutput ? portSourceNode.getOutputY() : portSourceNode.getInputY();
            drawBezierWire(canvas, sx, sy, dragCurrentX, dragCurrentY);
        }

        // 3) Nodes di atas kabel
        for (Node n : nodes) drawNode(canvas, n);
    }

    private void drawNode(Canvas canvas, Node n) {
        RectF r = new RectF(n.x, n.y, n.x + n.width, n.y + n.height);

        // body
        canvas.drawRoundRect(r, 10, 10, paintNodeBody);

        // header strip (slim category-color accent at top)
        RectF header = new RectF(n.x, n.y, n.x + n.width, n.y + 12);
        canvas.drawRoundRect(header, 10, 10, paintHeader);
        canvas.drawRect(n.x, n.y + 6, n.x + n.width, n.y + 12, paintHeader);

        // border (kategori warna atau gold kalo selected)
        if (n.selected) {
            canvas.drawRoundRect(r, 10, 10, paintNodeSelected);
        } else {
            Paint catBorder = new Paint(paintNodeBorder);
            catBorder.setColor(n.categoryColor);
            canvas.drawRoundRect(r, 10, 10, catBorder);
        }

        // Icon + label share one row (spec 4.6), vertically centered together.
        Bitmap iconBmp = getCategoryIcon(n.name);
        float iconSize = 22f;
        float iconX = n.x + 6;
        float rowCenter = n.y + n.height / 2f;
        float iconY = rowCenter - iconSize / 2f;
        float textX = iconX + iconSize + 6;
        // textX is ABSOLUTE (includes n.x); subtract the node-relative offset so
        // maxTextW stays the width available INSIDE this node, never negative.
        float maxTextW = n.width - (textX - n.x) - 22; // right padding clear of 12px output port glow
        String[] lines = wrapText(n.name, maxTextW);

        Paint.FontMetrics fm = paintText.getFontMetrics();

        if (iconBmp != null) {
            RectF dst = new RectF(iconX, iconY, iconX + iconSize, iconY + iconSize);
            canvas.drawBitmap(iconBmp, null, dst, null);
        }

        float lineH = fm.descent - fm.ascent;
        // First line centered on the same row as the icon; subsequent lines stack below.
        float baseline = rowCenter - (fm.ascent + fm.descent) / 2f;
        for (int li = 0; li < lines.length && li < 2; li++) {
            canvas.drawText(lines[li], textX, baseline, paintText);
            baseline += lineH;
        }

        // Port I/O dots (warna kategori)
        paintInputDot.setColor(n.categoryColor);
        paintOutputDot.setColor(n.categoryColor);
        int inX = n.getInputX(), inY = n.getInputY();
        int outX = n.getOutputX(), outY = n.getOutputY();

        // glow ring
        canvas.drawCircle(inX, inY, 12, paintInputDot);
        canvas.drawCircle(outX, outY, 12, paintOutputDot);
        // inner hole
        canvas.drawCircle(inX, inY, 6, paintNodeBody);
        canvas.drawCircle(outX, outY, 6, paintNodeBody);
        // center dot
        paintInputDot.setStrokeWidth(2f);
        paintOutputDot.setStrokeWidth(2f);
        paintInputDot.setStyle(Paint.Style.STROKE);
        paintOutputDot.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(inX, inY, 5, paintInputDot);
        canvas.drawCircle(outX, outY, 5, paintOutputDot);
        paintInputDot.setStyle(Paint.Style.FILL);
        paintOutputDot.setStyle(Paint.Style.FILL);

        // Delete button (×) cuma kalo dipilih
        if (n.selected) {
            float bx = n.x + n.width - 30;
            float by = n.y + 3;
            canvas.drawCircle(bx + 13, by + 13, 13, paintDeleteBtn);
            canvas.drawLine(bx + 6, by + 6, bx + 20, by + 20, paintDeleteX);
            canvas.drawLine(bx + 20, by + 6, bx + 6, by + 20, paintDeleteX);
        }
    }

    private String[] wrapText(String text, float maxWidth) {
        if (text == null) return new String[] {""};
        String full = paintText.measureText(text) <= maxWidth ? text : TextUtils.ellipsize(text, paintText, maxWidth * 2, TextUtils.TruncateAt.END).toString();
        if (paintText.measureText(full) <= maxWidth) return new String[] {full};
        // split into words
        String[] words = text.split(" ");
        java.util.List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String candidate = cur.length() == 0 ? w : cur + " " + w;
            if (paintText.measureText(candidate) <= maxWidth) {
                cur = new StringBuilder(candidate);
            } else {
                if (cur.length() > 0) { lines.add(cur.toString()); cur = new StringBuilder(); }
                // word longer than line: hard-truncate with ellipsis
                while (paintText.measureText(w) > maxWidth && w.length() > 1) {
                    w = w.substring(0, w.length() - 1);
                }
                lines.add(w.length() > 1 ? TextUtils.ellipsize(w, paintText, maxWidth, TextUtils.TruncateAt.END).toString() : w);
                cur = new StringBuilder();
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.isEmpty()) return new String[] {full};
        return lines.toArray(new String[0]);
    }

    private void drawBezierWire(Canvas canvas, float x1, float y1, float x2, float y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        float dx = Math.abs(x2 - x1);
        float cp = dx * 0.5f + 60;
        p.cubicTo(x1 + cp, y1, x2 - cp, y2, x2, y2);
        canvas.drawPath(p, paintWire);
    }
}