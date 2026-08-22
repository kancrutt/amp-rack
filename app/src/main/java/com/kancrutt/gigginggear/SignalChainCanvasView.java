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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalChainCanvasView extends View {

    public enum NodeType { PLUGIN, INPUT, OUTPUT, MERGE }

    public static class Node {
        public int id;
        public int pluginId;
        public String name;
        public float x;
        public float y;
        public float width = 74;
        public float height = 78;
        public boolean selected;
        public int categoryColor;
        public int categoryIconRes;
        public NodeType nodeType = NodeType.PLUGIN;

        public Node(int id, int pluginId, String name, float x, float y) {
            this.id = id;
            this.pluginId = pluginId;
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public boolean isAnchor() {
            return nodeType == NodeType.INPUT || nodeType == NodeType.OUTPUT || nodeType == NodeType.MERGE;
        }

        public boolean contains(float tx, float ty) {
            return tx >= x && tx <= x + width && ty >= y && ty <= y + height;
        }

        public boolean isInDeleteButton(float tx, float ty) {
            return isInDeleteButton(tx, ty, 1f);
        }

        /** Density-aware hit box matching drawNode's rendered X position. */
        public boolean isInDeleteButton(float tx, float ty, float density) {
            if (isAnchor()) return false;
            // Same geometry as drawNode: center (x+width-8d, y-4d), radius 9d
            float bx = x + width - 8 * density;
            float by = y - 4 * density;
            float r = 12 * density; // slightly larger than drawn 9d for touch comfort
            float dx = tx - bx;
            float dy = ty - by;
            return dx * dx + dy * dy <= r * r;
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
    public interface OnWireDeleteListener {
        void onWireDelete(Edge edge);
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Integer, Node> nodeMap = new HashMap<>();
    private final Map<String, Bitmap> iconCacheWithSize = new HashMap<>();
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
    private OnWireDeleteListener wireDeleteListener;

    private Node dragNode;
    private float lastX, lastY;
    private int dragFromIndex = -1;
    private boolean dragStarted = false;
    private static final float DRAG_THRESHOLD = 8f;

    private boolean draggingPort = false;
    private boolean draggingOutput = false;
    private Node portSourceNode = null;
    private float dragCurrentX, dragCurrentY;

    private Node outputAnchor = null;

    private static final int NODE_BG     = Color.parseColor("#1E1E22");
    private static final int NODE_BORDER = Color.parseColor("#3A3D45");
    private static final int NODE_SEL    = Color.parseColor("#FFAA00");
    private static final int HEADER_BG  = Color.parseColor("#2A2D34");
    private static final int TEXT_COLOR  = Color.parseColor("#E0E0E0");
    private static final int WIRE_COLOR  = Color.parseColor("#4A4D55");
    private static final int DOT_COLOR   = Color.parseColor("#FFAA00");
    private static final int PORT_RADIUS = 40;

    // Anchor colors
    private static final int COLOR_INPUT  = Color.parseColor("#00E676");
    private static final int COLOR_OUTPUT = Color.parseColor("#FF5252");
    private static final int COLOR_MERGE  = Color.parseColor("#FFAA00");

    public SignalChainCanvasView(Context ctx) { super(ctx); init(); }
    public SignalChainCanvasView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public SignalChainCanvasView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); init(); }

    private void init() {
        paintNodeBody.setColor(NODE_BG);
        paintNodeBorder.setColor(NODE_BORDER);
        paintNodeBorder.setStyle(Paint.Style.STROKE);
        paintNodeBorder.setStrokeWidth(3f);
        paintNodeSelected.setColor(NODE_SEL);
        paintNodeSelected.setStyle(Paint.Style.STROKE);
        paintNodeSelected.setStrokeWidth(6f);

        paintText.setColor(TEXT_COLOR);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        paintText.setTextSize(20f);

        paintWire.setColor(WIRE_COLOR);
        paintWire.setStyle(Paint.Style.STROKE);
        paintWire.setStrokeWidth(3.5f);

        paintInputDot.setColor(DOT_COLOR);
        paintOutputDot.setColor(DOT_COLOR);
        paintHeader.setColor(HEADER_BG);
        paintDeleteBtn.setColor(Color.parseColor("#FF5252"));
        paintDeleteX.setColor(Color.parseColor("#FFFFFF"));
        paintDeleteX.setStrokeWidth(2f);
        paintDeleteX.setStyle(Paint.Style.STROKE);
        paintTempWire.setColor(Color.parseColor("#FFAA00"));
        paintTempWire.setAntiAlias(true);
        paintTempWire.setStrokeWidth(3.5f);
        paintTempWire.setStyle(Paint.Style.STROKE);

        paintInputDot.setStyle(Paint.Style.FILL);
        paintOutputDot.setStyle(Paint.Style.FILL);
    }

    public static String getCategoryCode(String pluginName) {
        return CategoryHelper.getCode(pluginName);
    }

    public void setOnNodeSelectedListener(OnNodeSelectedListener l) { this.listener = l; }
    public void setOnChainChangedListener(OnChainChangedListener l) { this.chainChangedListener = l; }
    public void setOnDeleteNodeListener(OnDeleteNodeListener l) { this.deleteNodeListener = l; }
    public void setOnEdgeCreatedListener(OnEdgeCreatedListener l) { this.edgeCreatedListener = l; }
    public void setOnWireDeleteListener(OnWireDeleteListener l) { this.wireDeleteListener = l; }

    private int getCategoryColor(String pluginName) {
        return CategoryHelper.getColor(pluginName);
    }

    private int getCategoryIconRes(String pluginName) {
        return CategoryHelper.getIconRes(pluginName);
    }

    private Bitmap getCategoryIcon(String pluginName) {
        int resId = getCategoryIconRes(pluginName);
        // Rasterize at the EXACT on-screen pixel size (density-scaled). The old
        // fixed 44x44px bitmap was upscaled ~2.5-3x on modern phones -> blur.
        int iconPx = Math.round(36f * getResources().getDisplayMetrics().density);
        String cacheKey = resId + "@" + iconPx;
        Bitmap bmp = iconCacheWithSize.get(cacheKey);
        if (bmp == null) {
            try {
                Drawable d = ContextCompat.getDrawable(getContext(), resId);
                if (d != null) {
                    bmp = Bitmap.createBitmap(iconPx, iconPx, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    d.setBounds(0, 0, iconPx, iconPx);
                    int tint = getCategoryColor(pluginName);
                    if (tint != 0) {
                        d.mutate().setColorFilter(tint, android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                    d.draw(c);
                    iconCacheWithSize.put(cacheKey, bmp);
                }
            } catch (Exception e) {
                Log.w("SignalChainCanvas", "icon render fail for " + pluginName, e);
            }
        }
        return bmp;
    }

    public void loadFromActivePlugins() {
        // Save old nodes to preserve X/Y and selection
        Map<Integer, Node> oldNodes = new HashMap<>(nodeMap);
        
        nodes.clear();
        nodeMap.clear();
        iconCacheWithSize.clear();

        // Density-scale block geometry so blocks are physically the same size
        // on every DPI (previously raw pixels: tiny on xxhdpi, huge on ldpi)
        float density = getResources().getDisplayMetrics().density;

        int count = 0;
        try {
            count = AudioEngine.getActivePlugins();
        } catch (Exception e) {
            Log.e("SignalChainCanvas", "AudioEngine not ready", e);
        }

        int totalBlocks = 2 + count; // INPUT + count plugins + OUTPUT
        float viewW = getWidth() > 0 ? getWidth() : (380 * density);
        float fabReserved = 85 * density; // Margin for FAB + padding
        float usableWidth = Math.max(200 * density, viewW - fabReserved);

        // Responsive node size: scale down slightly if many blocks exist (e.g. 5-6 blocks)
        float nodeWDp = (totalBlocks >= 6) ? 56f : (totalBlocks >= 5 ? 62f : 68f);
        float nodeHDp = (totalBlocks >= 6) ? 62f : (totalBlocks >= 5 ? 68f : 72f);
        float nodeW = nodeWDp * density;
        float nodeH = nodeHDp * density;

        float totalNodesWidth = totalBlocks * nodeW;
        float dynamicSpacing = (usableWidth - totalNodesWidth) / Math.max(1, totalBlocks - 1);
        float spacing = Math.max(6 * density, Math.min(32 * density, dynamicSpacing));

        float xCursor = 8 * density;
        float yCenter = getHeight() > 0 ? getHeight() / 2f : 48f * density;

        // 1) INPUT anchor (always first, non-deletable)
        float inputX = xCursor;
        float inputY = yCenter - nodeH / 2f;
        Node inputNode = new Node(-100, -1, "INPUT", inputX, inputY);
        inputNode.nodeType = NodeType.INPUT;
        inputNode.categoryColor = COLOR_INPUT;
        inputNode.width = nodeW;
        inputNode.height = nodeH;
        nodes.add(inputNode);
        nodeMap.put(inputNode.id, inputNode);
        xCursor += nodeW + spacing;

        // 2) Plugin blocks from AudioEngine

        for (int i = 0; i < count; i++) {
            String name = null;
            try { name = AudioEngine.getActivePluginName(i); } catch (Exception e) { Log.w("SignalChainCanvas", "name fail i=" + i, e); }
            if (name == null) name = "Plugin " + i;

            float cx = xCursor;
            float cy = yCenter - nodeH / 2f;
            xCursor += nodeW + spacing;

            Node n = new Node(i, AudioEngine.getActivePluginID(i), name, cx, cy);
            n.categoryColor = getCategoryColor(name);
            n.categoryIconRes = getCategoryIconRes(name);
            n.width = nodeW;
            n.height = nodeH;
            
            // Preserve selection
            if (oldNodes.containsKey(i)) {
                n.selected = oldNodes.get(i).selected;
            }
            
            nodes.add(n);
            nodeMap.put(n.id, n);
        }

        // 3) OUTPUT anchor
        float outX = xCursor;
        float outY = yCenter - nodeH / 2f;
        Node outputNode = new Node(-200, -1, "OUTPUT", outX, outY);
        outputNode.nodeType = NodeType.OUTPUT;
        outputNode.categoryColor = COLOR_OUTPUT;
        outputNode.width = nodeW;
        outputNode.height = nodeH;
        nodes.add(outputNode);
        nodeMap.put(outputNode.id, outputNode);
        outputAnchor = outputNode;

        // Safety clamp: all nodes inside canvas bounds (fixes dangling off-canvas lines)
        if (getWidth() > 0 && getHeight() > 0) {
            for (Node n : nodes) {
                n.x = Math.max(0, Math.min(n.x, getWidth() - n.width));
                n.y = Math.max(0, Math.min(n.y, getHeight() - n.height));
            }
        }

        // Clean up orphaned edges (edges pointing to nodes that were deleted)
        List<Edge> validEdges = new ArrayList<>();
        for (Edge e : edges) {
            if (nodeMap.containsKey(e.fromNodeId) && nodeMap.containsKey(e.toNodeId)) {
                validEdges.add(e);
            }
        }
        edges.clear();
        edges.addAll(validEdges);

        // If NO edges exist, auto-wire them in series
        if (edges.isEmpty()) {
            int lastPluginNodeId = inputNode.id;
            for (int i = 0; i < count; i++) {
                edges.add(new Edge(lastPluginNodeId, i));
                lastPluginNodeId = i;
            }
            edges.add(new Edge(lastPluginNodeId, outputNode.id));
        }

        syncGraphToAudioEngine();

        requestLayout();
        invalidate();
    }

    private void syncGraphToAudioEngine() {
        int[] from = new int[edges.size()];
        int[] to = new int[edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            from[i] = edges.get(i).fromNodeId;
            to[i] = edges.get(i).toNodeId;
        }
        try {
            AudioEngine.setGraph(from, to);
        } catch (Exception e) {
            Log.e("SignalChainCanvas", "setGraph failed", e);
        }
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

    private float portRadiusPx() {
        return PORT_RADIUS * getResources().getDisplayMetrics().density;
    }

    private boolean isNear(Node n, float tx, float ty, boolean output) {
        float px = output ? n.getOutputX() : n.getInputX();
        float py = output ? n.getOutputY() : n.getInputY();
        float dx = tx - px;
        float dy = ty - py;
        float r = portRadiusPx();
        return dx * dx + dy * dy < r * r;
    }

    private Node findNearestPort(float tx, float ty, boolean lookingForInput, Node exclude) {
        Node best = null;
        float r = portRadiusPx() * 2; // generous snap target for wire creation
        float bestDist = r * r;
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

    private Edge findEdgeAt(float tx, float ty) {
        float threshold = 14 * getResources().getDisplayMetrics().density;
        for (Edge e : edges) {
            Node a = nodeMap.get(e.fromNodeId);
            Node b = nodeMap.get(e.toNodeId);
            if (a == null || b == null) continue;
            float x1 = a.getOutputX();
            float y1 = a.getOutputY();
            float x2 = b.getInputX();
            float y2 = b.getInputY();
            float bend = Math.abs(x2 - x1) * 0.5f;
            for (int i = 0; i <= 20; i++) {
                float t = i / 20f;
                float mt = 1 - t;
                float bx = mt * mt * mt * x1 + 3 * mt * mt * t * (x1 + bend) + 3 * mt * t * t * (x2 - bend) + t * t * t * x2;
                float by = mt * mt * mt * y1 + 3 * mt * mt * t * y1 + 3 * mt * t * t * y2 + t * t * t * y2;
                float ddx = tx - bx;
                float ddy = ty - by;
                if (ddx * ddx + ddy * ddy < threshold * threshold) return e;
            }
        }
        return null;
    }

    public void deleteEdge(Edge e) {
        edges.remove(e);
        syncGraphToAudioEngine();
        invalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Node sel = getSelectedNode();
                if (sel != null && !sel.isAnchor() && sel.isInDeleteButton(tx, ty, getResources().getDisplayMetrics().density)) {
                    if (deleteNodeListener != null) deleteNodeListener.onDeleteNode(sel);
                    return true;
                }
                // Cek BLOCK dulu — drag block prioritas utama
                Node hit = hitTest(tx, ty);
                if (hit != null && hit.nodeType != NodeType.OUTPUT) {
                    // Tap di dalam block = SELECT + siap drag (bukan port)
                    for (Node n : nodes) n.selected = false;
                    hit.selected = true;
                    lastX = tx;
                    lastY = ty;
                    dragFromIndex = hit.id;
                    dragStarted = false;
                    if (listener != null) listener.onNodeSelected(hit);
                    dragNode = hit;
                    invalidate();
                    return true;
                }

                // Cek port (hanya kalau tepat di pinggir 20px block)
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
                // Cek WIRE — tap di dekat garis koneksi = disconnect
                Edge wireHit = findEdgeAt(tx, ty);
                if (wireHit != null) {
                    if (wireDeleteListener != null) wireDeleteListener.onWireDelete(wireHit);
                    return true;
                }
                return true;
                case MotionEvent.ACTION_MOVE:
                if (draggingPort) {
                    dragCurrentX = tx;
                    dragCurrentY = ty;
                    invalidate();
                    return true;
                }
                if (dragNode != null && !dragNode.isAnchor()) {
                    if (!dragStarted) {
                        float dx = tx - lastX;
                        float dy = ty - lastY;
                        if (dx * dx + dy * dy > DRAG_THRESHOLD * DRAG_THRESHOLD) {
                            dragStarted = true;
                        }
                    }
                    if (dragStarted) {
                        float dx = tx - lastX;
                        float dy = ty - lastY;
                        float nx = dragNode.x + dx;
                        float ny = dragNode.y + dy;
                        // Clamp ke dalam bounds canvas (A1)
                        nx = Math.max(0, Math.min(nx, getWidth() - dragNode.width));
                        ny = Math.max(0, Math.min(ny, getHeight() - dragNode.height));
                        dragNode.x = nx;
                        dragNode.y = ny;

                        lastX = tx;
                        lastY = ty;
                        invalidate();
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingPort && portSourceNode != null) {
                    Node target = findNearestPort(dragCurrentX, dragCurrentY, draggingOutput, portSourceNode);
                    if (target != null) {
                        Edge newEdge = draggingOutput
                                ? new Edge(portSourceNode.id, target.id)
                                : new Edge(target.id, portSourceNode.id);
                        boolean exists = false;
                        for (Edge e : edges) {
                            if (e.fromNodeId == newEdge.fromNodeId && e.toNodeId == newEdge.toNodeId) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            edges.add(newEdge);
                            syncGraphToAudioEngine();
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
                // Auto-justify snap on DROP
                if (dragNode != null && !dragNode.isAnchor() && dragStarted) {
                    float density = getResources().getDisplayMetrics().density;
                    float colWidth = (dragNode.width > 0 ? dragNode.width : 56 * density) + 12 * density;
                    float rowHeight = (dragNode.height > 0 ? dragNode.height : 62 * density) + 10 * density;
                    float yCenter = getHeight() / 2f - dragNode.height / 2f;
                    int row = Math.round((dragNode.y - yCenter) / rowHeight);
                    dragNode.y = yCenter + (row * rowHeight);
                    int col = Math.round((dragNode.x - 8 * density) / colWidth);
                    if (col < 0) col = 0;
                    dragNode.x = 8 * density + (col * colWidth);

                    boolean overlap;
                    do {
                        overlap = false;
                        for (Node n : nodes) {
                            if (n != dragNode && Math.abs(n.x - dragNode.x) < 15 * density && Math.abs(n.y - dragNode.y) < 15 * density) {
                                dragNode.x += colWidth;
                                overlap = true;
                                break;
                            }
                        }
                    } while (overlap);
                    // Clamp snap ke dalam bounds canvas
                    dragNode.x = Math.max(0, Math.min(dragNode.x, getWidth() - dragNode.width));
                    dragNode.y = Math.max(0, Math.min(dragNode.y, getHeight() - dragNode.height));
                    syncGraphToAudioEngine();
                }

                dragNode = null;
                dragFromIndex = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }

    public Node getSelectedNode() {
        for (Node n : nodes) if (n.selected) return n;
        return null;
    }

    public void setCustomNodePositions(Map<String, float[]> customPos) {
        if (customPos == null || customPos.isEmpty()) return;
        for (Node n : nodes) {
            String key = String.valueOf(n.id);
            if (customPos.containsKey(key)) {
                float[] xy = customPos.get(key);
                if (xy != null && xy.length >= 2) {
                    n.x = xy[0];
                    n.y = xy[1];
                }
            }
        }
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (outputAnchor != null) {
            outputAnchor.y = getHeight() / 2f - outputAnchor.height / 2f;
        }

        for (Edge e : edges) {
            Node a = nodeMap.get(e.fromNodeId);
            Node b = nodeMap.get(e.toNodeId);
            if (a == null || b == null) continue;
            int wireColor = a.isAnchor() ? WIRE_COLOR : a.categoryColor;
            drawBezierWire(canvas, a.getOutputX(), a.getOutputY(), b.getInputX(), b.getInputY(), wireColor);
        }

        if (draggingPort && portSourceNode != null) {
            int wireColor = portSourceNode.isAnchor() ? WIRE_COLOR : portSourceNode.categoryColor;
            drawBezierWire(canvas,
                    draggingOutput ? portSourceNode.getOutputX() : portSourceNode.getInputX(),
                    draggingOutput ? portSourceNode.getOutputY() : portSourceNode.getInputY(),
                    dragCurrentX, dragCurrentY, wireColor);
        }

        for (Node n : nodes) {
            if (n.isAnchor()) {
                drawAnchorNode(canvas, n);
            } else {
                drawNode(canvas, n);
            }
        }
    }

    private void drawAnchorNode(Canvas canvas, Node n) {
        RectF r = new RectF(n.x, n.y, n.x + n.width, n.y + n.height);

        // Body
        paintNodeBody.setColor(NODE_BG);
        canvas.drawRoundRect(r, 12, 12, paintNodeBody);

        // Border — color per type
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(3f);
        border.setColor(n.categoryColor);
        canvas.drawRoundRect(r, 12, 12, border);

        // Glyph in center
        Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glyphPaint.setColor(n.categoryColor);
        glyphPaint.setStyle(Paint.Style.FILL);

        float cx = n.x + n.width / 2f;
        float cy = n.y + n.height / 2f;

        if (n.nodeType == NodeType.INPUT) {
            // Right-pointing triangle (guitar input)
            Path tri = new Path();
            tri.moveTo(cx - 10, cy - 12);
            tri.lineTo(cx + 10, cy);
            tri.lineTo(cx - 10, cy + 12);
            tri.close();
            canvas.drawPath(tri, glyphPaint);
        } else if (n.nodeType == NodeType.OUTPUT) {
            // Speaker cone shape
            canvas.drawCircle(cx, cy, 10, glyphPaint);
            Paint dotCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotCenter.setColor(NODE_BG);
            canvas.drawCircle(cx, cy, 4, dotCenter);
        } else if (n.nodeType == NodeType.MERGE) {
            // Two arrows merging downward
            Path merge = new Path();
            merge.moveTo(cx - 12, cy - 8);
            merge.lineTo(cx, cy + 4);
            merge.lineTo(cx + 12, cy - 8);
            merge.close();
            canvas.drawPath(merge, glyphPaint);
            canvas.drawCircle(cx, cy + 10, 3, glyphPaint);
        }

        // Label (anchor nodes: INPUT, OUTPUT)
        paintText.setColor(n.categoryColor);
        paintText.setTextSize(11f * getResources().getDisplayMetrics().density);
        canvas.drawText(n.name, cx, n.y + n.height - 8, paintText);

        // Port dots — radius density-scaled (3.5 raw px was ~1dp on 3x screens: invisible)
        float pr = 4.5f * getResources().getDisplayMetrics().density;
        paintInputDot.setColor(n.categoryColor);
        paintOutputDot.setColor(n.categoryColor);
        canvas.drawCircle(n.getInputX(), n.getInputY(), pr, paintInputDot);
        canvas.drawCircle(n.getOutputX(), n.getOutputY(), pr, paintOutputDot);
    }

    private void drawNode(Canvas canvas, Node n) {
        RectF r = new RectF(n.x, n.y, n.x + n.width, n.y + n.height);

        paintNodeBody.setColor(Color.parseColor("#1E1E22"));
        canvas.drawRoundRect(r, 12, 12, paintNodeBody);

        if (n.selected) {
            Paint selGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            selGlow.setStyle(Paint.Style.STROKE);
            selGlow.setStrokeWidth(5f);
            selGlow.setColor(n.categoryColor);
            canvas.drawRoundRect(r, 12, 12, selGlow);
        } else {
            Paint normBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            normBorder.setStyle(Paint.Style.STROKE);
            normBorder.setStrokeWidth(2.5f);
            normBorder.setColor(n.categoryColor);
            canvas.drawRoundRect(r, 12, 12, normBorder);
        }

        Bitmap iconBmp = getCategoryIcon(n.name);
        float d = getResources().getDisplayMetrics().density;
        float iconSize = 36f * d;
        float iconX = n.x + (n.width - iconSize) / 2f;
        float iconY = n.y + 7f * d;

        if (iconBmp != null) {
            RectF dst = new RectF(iconX, iconY, iconX + iconSize, iconY + iconSize);
            canvas.drawBitmap(iconBmp, null, dst, null);
        }

        // Label (compact, crisp, inside block bottom)
        String code = getCategoryCode(n.name);
        paintText.setColor(Color.parseColor("#ECEFF1"));
        paintText.setTextSize(12f * d);
        canvas.drawText(code, n.x + n.width / 2f, n.y + n.height - 8f * d, paintText);

        // Port dots — density-scaled to match anchor nodes
        float pr = 4.5f * getResources().getDisplayMetrics().density;
        paintInputDot.setColor(n.categoryColor);
        paintOutputDot.setColor(n.categoryColor);
        canvas.drawCircle(n.getInputX(), n.getInputY(), pr, paintInputDot);
        canvas.drawCircle(n.getOutputX(), n.getOutputY(), pr, paintOutputDot);

        if (n.selected) {
            float bx = n.x + n.width - 8 * d;
            float by = n.y - 4 * d;
            canvas.drawCircle(bx, by, 9 * d, paintDeleteBtn);
            canvas.drawLine(bx - 3.5f * d, by - 3.5f * d, bx + 3.5f * d, by + 3.5f * d, paintDeleteX);
            canvas.drawLine(bx + 3.5f * d, by - 3.5f * d, bx - 3.5f * d, by + 3.5f * d, paintDeleteX);
        }
    }

    private void drawBezierWire(Canvas canvas, float x1, float y1, float x2, float y2, int color) {
        Path path = new Path();
        path.moveTo(x1, y1);
        float dx = Math.abs(x2 - x1) * 0.5f;
        path.cubicTo(x1 + dx, y1, x2 - dx, y2, x2, y2);

        paintWire.setColor(WIRE_COLOR);
        canvas.drawPath(path, paintWire);

        if (x2 - x1 > 20) {
            float midX = (x1 + x2) / 2f;
            float midY = (y1 + y2) / 2f;
            Path arrow = new Path();
            arrow.moveTo(midX - 4, midY - 4);
            arrow.lineTo(midX + 3, midY);
            arrow.lineTo(midX - 4, midY + 4);
            arrow.close();
            Paint pArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
            pArrow.setColor(Color.parseColor("#8F94A0"));
            pArrow.setStyle(Paint.Style.FILL);
            canvas.drawPath(arrow, pArrow);
        }
    }
}
