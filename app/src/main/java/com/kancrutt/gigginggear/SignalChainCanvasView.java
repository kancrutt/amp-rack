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
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalChainCanvasView extends View {

    public enum NodeType { PLUGIN, INPUT, OUTPUT, SPLIT, MERGE }

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
        public String categoryCode;
        public NodeType nodeType = NodeType.PLUGIN;
        public boolean bypassed;
        /** 0 = main trunk, 1 = upper parallel branch, 2 = lower parallel branch. */
        public int branch = 0;

        public Node(int id, int pluginId, String name, float x, float y) {
            this.id = id;
            this.pluginId = pluginId;
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public boolean isAnchor() {
            // Only INPUT/OUTPUT are invisible, non-interactive engine anchors.
            // SPLIT/MERGE are real routing plugin blocks: visible, selectable, deletable.
            return nodeType == NodeType.INPUT || nodeType == NodeType.OUTPUT;
        }

        public boolean isRouting() {
            return nodeType == NodeType.SPLIT || nodeType == NodeType.MERGE;
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
    public interface OnParallelDropListener {
        /** A trunk block was dropped off the series row: wrap it in SPLIT/MERGE. */
        void onParallelDrop(int slotIndex);
    }
    public interface OnReorderListener {
        /** The user dragged a block to a new horizontal slot: the ordered slot
         *  list (left -> right, entries are pre-move engine slots) is the new
         *  signal order. Block order == signal order. */
        void onReorder(int[] orderedOldSlots);
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
    private OnParallelDropListener parallelDropListener;
    private OnReorderListener reorderListener;

    private Node dragNode;
    // Series wires are locked while a block is being dragged. Keep the wire
    // endpoints from ACTION_DOWN instead of following the moving node.
    private final Map<Integer, float[]> dragWirePositions = new HashMap<>();
    private float lastX, lastY;
    private int dragFromIndex = -1;
    private boolean dragStarted = false;
    private long lastTapTime = 0;
    private int lastTapNodeId = -1;
    private Map<String, float[]> pendingCustomPos = null;
    /** R19: node count at last rebuild — grow == a block was added (scroll
     *  the phone canvas to the new tail); first layout uses -1 (no scroll). */
    private int prevNodeCount = -1;
    private static final float DRAG_THRESHOLD = 8f;
    // Single-flight guard: coalesce redundant rebuilds within 800ms
    private long lastLoadMs = 0;
    private String lastLoadedFp = "";

    private boolean draggingPort = false;
    private boolean draggingOutput = false;
    private Node portSourceNode = null;
    private float dragCurrentX, dragCurrentY;

    private Node outputAnchor = null;
    private Node inputAnchor = null;

    // Phone-class scrollable canvas (R.bool.canvas_phone_scroll: true below
    // 600dp smallestWidth, false in values-sw600dp). Every phone branch below
    // is gated on this flag — tablet always takes the original code path.
    private boolean phoneScroll = false;
    /** Phone: full scrollable content size (series + branch rows, full chain width). */
    private float contentWPx = 0f;
    private float contentHPx = 0f;
    /** Phone: vertical pitch of canvas rows + whether a SPLIT/MERGE pair exists. */
    private float contentRowPitchPx = 0f;
    private boolean lastHadParallel = false;

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    private boolean readPhoneScrollMode() {
        try {
            return getResources().getBoolean(R.bool.canvas_phone_scroll);
        } catch (Exception e) {
            return false;
        }
    }

    /** Phone: content height computed at load (0 = unknown / tablet N/A). */
    public float getPhoneContentHPx() {
        return phoneScroll ? contentHPx : 0f;
    }

    // TONE3000 compact T3K mark — per spec §16/25: compact T3K in signal-chain block
    // Badge set is keyed by stable pluginId (AudioEngine.getActivePluginID), not by
    // volatile node id (slot index). This survives slot shifts from delete/move.
    private final java.util.Set<Integer> t3kNodeIds = new java.util.HashSet<>();
    // Current slot -> pluginId mapping (rebuilt on each loadFromActivePlugins).
    private final java.util.Map<Integer, Integer> slotToPluginId = new java.util.HashMap<>();
    public void setT3kBadge(int id, boolean isT3k) {
        // Resolve id to stable pluginId if it's a current slot index.
        int pluginId = slotToPluginId.containsKey(id) ? slotToPluginId.get(id) : id;
        if (isT3k) t3kNodeIds.add(pluginId); else t3kNodeIds.remove(pluginId);
        invalidate();
    }
    public void clearT3kBadges() { t3kNodeIds.clear(); invalidate(); }
    public boolean isT3kNode(int id) {
        int pluginId = slotToPluginId.containsKey(id) ? slotToPluginId.get(id) : id;
        return t3kNodeIds.contains(pluginId);
    }

    /** Re-apply T3K badges from a slot-keyed metadata map (e.g. t3kBlockMeta).
     *  Clears current badges first, then badges every slot present in the map by
     *  resolving slot -> pluginId via the current slotToPluginId map. Call only
     *  AFTER slotToPluginId has been rebuilt (e.g. right after loadFromActivePlugins). */
    public void reapplyT3kBadgesFromMeta(java.util.Map<Integer, ?> metaBySlot) {
        t3kNodeIds.clear();
        if (metaBySlot == null) { invalidate(); return; }
        for (Integer slot : metaBySlot.keySet()) {
            Integer pid = slotToPluginId.get(slot);
            if (pid != null) t3kNodeIds.add(pid);
        }
        invalidate();
    }

    private static final int NODE_BG     = Color.parseColor("#1F2228");
    private static final int NODE_BORDER = Color.parseColor("#2E323B");
    private static final int NODE_SEL    = Color.parseColor("#FF8A00");
    private static final int HEADER_BG  = Color.parseColor("#17191E");
    private static final int TEXT_COLOR  = Color.parseColor("#E6E6E6");
    private static final int WIRE_COLOR  = Color.parseColor("#52525B");
    private static final int DOT_COLOR   = Color.parseColor("#FF8A00");
    private static final int PORT_RADIUS = 40;

    // Anchor colors
    private static final int COLOR_INPUT  = Color.parseColor("#22C55E");
    private static final int COLOR_OUTPUT = Color.parseColor("#EF4444");
    private static final int COLOR_ROUTING = Color.parseColor("#A1A1AA");
    // R17 §2.3: Helix-style branch colors — upper branch (A) blue, lower
    // branch (B) amber. Wires, port dots and tile borders carry them so the
    // two parallel paths are readable at a glance (previously only a thin
    // category-colored dot distinguished them).
    private static final int COLOR_BRANCH_A = Color.parseColor("#4C8DFF");
    private static final int COLOR_BRANCH_B = Color.parseColor("#E3B341");
    /** Branch identity of a chain slot, or 0 when it is not in a branch. */
    static int branchAccentColor(int branch) {
        if (branch == 1) return COLOR_BRANCH_A;
        if (branch == 2) return COLOR_BRANCH_B;
        return 0;
    }

    public SignalChainCanvasView(Context ctx) { super(ctx); init(); }
    public SignalChainCanvasView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public SignalChainCanvasView(Context ctx, AttributeSet attrs, int defStyle) { super(ctx, attrs, defStyle); init(); }

    /** Visible viewport height of the phone scroll card (parent chain:
     *  canvas -> HorizontalScrollView -> NestedScrollView). Falls back to the
     *  signal_chain_card_height dimen = the card's exact height. */
    private float viewportHPx() {
        try {
            android.view.ViewParent p = getParent();
            if (p instanceof android.view.View) {
                android.view.ViewParent pp = ((android.view.View) p).getParent();
                if (pp instanceof android.view.View) {
                    int h = ((android.view.View) pp).getHeight();
                    if (h > 0) return h;
                }
            }
        } catch (Exception ignore) {}
        try {
            // Phone reads the sw-independent card height; tablet keeps the
            // historical w-ladder value (160/190dp).
            return getResources().getDimensionPixelSize(phoneScroll
                    ? R.dimen.signal_chain_card_height
                    : R.dimen.signal_chain_height);
        } catch (Exception e) {
            return 85 * density();
        }
    }

    private float viewportWPx() {
        try {
            android.view.ViewParent p = getParent();
            if (p instanceof android.view.View) {
                int w = ((android.view.View) p).getWidth();
                if (w > 0) return w;
            }
        } catch (Exception ignore) {}
        if (getWidth() > 0) return getWidth();
        return 380 * density();
    }

    private void init() {
        phoneScroll = readPhoneScrollMode();
        paintNodeBody.setColor(NODE_BG);
        paintNodeBorder.setColor(NODE_BORDER);
        paintNodeBorder.setStyle(Paint.Style.STROKE);
        paintNodeBorder.setStrokeWidth(3f);
        paintNodeSelected.setColor(NODE_SEL);
        paintNodeSelected.setStyle(Paint.Style.STROKE);
        paintNodeSelected.setStrokeWidth(6f);

        paintText.setColor(TEXT_COLOR);
        paintText.setTextAlign(Paint.Align.CENTER);
                try {
            android.graphics.Typeface tf = androidx.core.content.res.ResourcesCompat.getFont(getContext(), R.font.plus_jakarta_sans);
            if (tf != null) {
                paintText.setTypeface(android.graphics.Typeface.create(tf, android.graphics.Typeface.BOLD));
            } else {
                paintText.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
            }
        } catch (Throwable t) {
            paintText.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        }
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

    public void selectNode(int id) {
        for (Node n : nodes) {
            n.selected = (n.id == id);
        }
        invalidate();
    }

    /** Highlight the tile at a chain slot (post-load order matches engine
     *  order). selectNode takes a pluginId, which is shared by duplicate LV2
     *  blocks and unrelated to slot indexes — add-flows need this one. */
    public void selectSlot(int slot) {
        // nodes[] = [INPUT anchor, plugin0, plugin1, ..., OUTPUT anchor], so
        // plugin slot s lives at index s+1. Matching i==slot was off by one:
        // the add-flow highlighted the PREVIOUS block and left the new one
        // unselected (round-13 R2.7: panel moved but rack marker didn't).
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            n.selected = !n.isAnchor() && (i - 1) == slot;
        }
        invalidate();
    }

    public void setOnNodeSelectedListener(OnNodeSelectedListener l) { this.listener = l; }
    public void setOnChainChangedListener(OnChainChangedListener l) { this.chainChangedListener = l; }
    public void setOnDeleteNodeListener(OnDeleteNodeListener l) { this.deleteNodeListener = l; }
    public void setOnEdgeCreatedListener(OnEdgeCreatedListener l) { this.edgeCreatedListener = l; }
    public void setOnWireDeleteListener(OnWireDeleteListener l) { this.wireDeleteListener = l; }
    public void setOnParallelDropListener(OnParallelDropListener l) { this.parallelDropListener = l; }
    public void setOnReorderListener(OnReorderListener l) { this.reorderListener = l; }

    private int getCategoryColor(String pluginName) {
        return CategoryHelper.getColor(pluginName);
    }

    private int getCategoryIconRes(String pluginName) {
        return CategoryHelper.getIconRes(pluginName);
    }

    private Bitmap getCategoryIcon(String pluginName) {
        int resId = getCategoryIconRes(pluginName);
        return getCategoryIconFromRes(resId, getCategoryColor(pluginName), pluginName);
    }

    private Bitmap getCategoryIconFromRes(int resId, int tint, String cacheName) {
        // Rasterize at the EXACT on-screen pixel size (density-scaled). The old
        // fixed 44x44px bitmap was upscaled ~2.5-3x on modern phones -> blur.
        int iconPx = Math.round(36f * getResources().getDisplayMetrics().density);
        String cacheKey = cacheName + "@" + resId + "@" + iconPx + "@" + tint;
        Bitmap bmp = iconCacheWithSize.get(cacheKey);
        if (bmp == null) {
            try {
                Drawable d = ContextCompat.getDrawable(getContext(), resId);
                if (d != null) {
                    bmp = Bitmap.createBitmap(iconPx, iconPx, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    d.setBounds(0, 0, iconPx, iconPx);
                    if (tint != 0) {
                        d.mutate().setColorFilter(tint, android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                    d.draw(c);
                    iconCacheWithSize.put(cacheKey, bmp);
                }
            } catch (Exception e) {
                Log.w("SignalChainCanvas", "icon render fail for " + cacheName, e);
            }
        }
        return bmp;
    }

    public void loadFromActivePlugins() {
        loadFromActivePlugins(false);
    }
    public void loadFromActivePlugins(boolean force) {
        StringBuilder sbBefore = new StringBuilder();
        for (Node n : nodes) {
            sbBefore.append(n.id).append("/").append(n.name).append("/").append(n.x).append("/").append(n.y).append(";");
        }
        // Single-flight: skip duplicate rebuild for identical engine state within 800ms
        try {
            StringBuilder fp = new StringBuilder();
            int c = 0;
            try { c = AudioEngine.getActivePlugins(); } catch (Exception ignored) {}
            fp.append(c).append("|");
            for (int i = 0; i < c; i++) {
                try { fp.append(AudioEngine.getActivePluginID(i)).append(":").append(AudioEngine.getActivePluginName(i)).append(";"); }
                catch (Exception ignored) { fp.append(i).append(":?;"); }
            }
            // Pending restore must not be skipped — it carries new position state
            boolean hasPending = pendingCustomPos != null && !pendingCustomPos.isEmpty();
            String curFp = fp.toString() + "|pending=" + hasPending;
            long now = android.os.SystemClock.uptimeMillis();
            if (!force && !hasPending && curFp.equals(lastLoadedFp) && (now - lastLoadMs) < 800) {
                return;
            }
            lastLoadedFp = curFp;
            lastLoadMs = now;
        } catch (Exception ignored) {}
        // Save old nodes to preserve X/Y and selection
        Map<Integer, Node> oldNodes = new HashMap<>(nodeMap);
        // Preserve visual state per plugin id, but as a FIFO queue per id:
        // every LV2 plugin reports UniqueID 0 and two instances of one LADSPA
        // plugin share an id, so a single Node-per-id map made every LV2 block
        // "inherit" the same (last-wins) old node — selection stuck on the
        // wrong tile after add/reorder. kth block of an id now gets the kth
        // old state (ordered by slot so the queue is deterministic).
        Map<Integer, java.util.ArrayDeque<Node>> oldByPluginId = new HashMap<>();
        {
            List<Node> oldList = new ArrayList<>();
            for (Node old : oldNodes.values()) {
                if (old != null && !old.isAnchor()) oldList.add(old);
            }
            oldList.sort(java.util.Comparator.comparingInt(o -> o.id));
            for (Node old : oldList) {
                oldByPluginId.computeIfAbsent(old.pluginId, k -> new java.util.ArrayDeque<>()).addLast(old);
            }
        }
        
        nodes.clear();
        nodeMap.clear();
        iconCacheWithSize.clear();
        slotToPluginId.clear();

        // Density-scale block geometry so blocks are physically the same size
        // on every DPI (previously raw pixels: tiny on xxhdpi, huge on ldpi)
        float density = getResources().getDisplayMetrics().density;

        int count = 0;
        try {
            count = AudioEngine.getActivePlugins();
        } catch (Exception e) {
            Log.e("SignalChainCanvas", "AudioEngine not ready", e);
        }
        StringBuilder sbPlugins = new StringBuilder();
        for (int i = 0; i < count; i++) {
            try {
                String name = AudioEngine.getActivePluginName(i);
                sbPlugins.append(i).append("/").append(name).append(";");
            } catch (Exception ignored) {}
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
        if (phoneScroll) {
            // Phone compact blocks: the card shows a single center row, so
            // blocks shrink and the freed vertical space goes to the parameter
            // panel below. Tablet keeps full-size blocks.
            nodeWDp = 54f;
            nodeHDp = 56f;
            nodeW = nodeWDp * density;
            nodeH = nodeHDp * density;
        }

        // Phone scrollable canvas: fixed 3-row content when a SPLIT/MERGE pair
        // creates branch rows (series row centered = home view, branches
        // above/below via v-scroll), single viewport-fitted row otherwise.
        // Blocks are never shrunk to fit — the view scrolls instead.
        // Tablet keeps the original shrink-to-fit-3-rows behavior.
        boolean hasParallel = false;
        if (phoneScroll) {
            for (int i = 0; i < count; i++) {
                String nm = null;
                try { nm = AudioEngine.getActivePluginName(i); } catch (Exception ignore) {}
                if (nm != null) {
                    String nl = nm.toLowerCase();
                    if (nl.contains("split") || nl.contains("merge")) { hasParallel = true; break; }
                }
            }
            float vH = viewportHPx();
            // Always 3 rows on phone: the empty upper/lower rows are the drop
            // zones for the drag-to-parallel gesture (series home view stays
            // centered). Dashed guides hint at them while dragging.
            float rowPitch = nodeH + 12f * density;
            contentRowPitchPx = rowPitch;
            lastHadParallel = hasParallel;
            contentHPx = Math.max(vH, 3f * rowPitch + 28f * density);
        } else if (getHeight() > 0) {
            // Guarantee 3 rows fit top-to-bottom: shrink node + glyph when canvas
            // is too short for the default height (S23 landscape ~85dp).
            float maxH = getHeight() / 3f - 4f * density;
            if (nodeH > maxH && maxH > 30f * density) {
                float ratio = maxH / nodeH;
                nodeW = nodeW * ratio;
                nodeH = maxH;
            }
        }

        float totalNodesWidth = totalBlocks * nodeW;
        float dynamicSpacing = (usableWidth - totalNodesWidth) / Math.max(1, totalBlocks - 1);
        float spacing = Math.max(6 * density, Math.min(32 * density, dynamicSpacing));

        float xCursor = 8 * density;
        // Phone: series row sits at the vertical center of the scrollable
        // content (= the centered "home" view). Tablet: viewport center.
        float yCenter = phoneScroll ? contentHPx / 2f
                : (getHeight() > 0 ? getHeight() / 2f : 48f * density);

        // 1) INPUT anchor: smaller visual marker, fixed to far left
        float anchorDim = 24 * density;
        Node inputNode = new Node(-100, -1, "IN", 4 * density, yCenter - anchorDim / 2f);
        inputNode.nodeType = NodeType.INPUT;
        inputNode.categoryColor = COLOR_INPUT;
        inputNode.width = anchorDim;
        inputNode.height = anchorDim;
        inputAnchor = inputNode;
        nodes.add(inputNode);
        nodeMap.put(inputNode.id, inputNode);
        xCursor = inputNode.x + inputNode.width + spacing;

        // 2) Plugin blocks from AudioEngine

        // Find rightmost position among existing blocks to place new blocks cleanly next to the last one
        float rightmostX = inputNode.x + inputNode.width;
        float lastBlockY = yCenter - nodeH / 2f;
        for (Map.Entry<Integer, Node> entry : oldNodes.entrySet()) {
            if (entry.getKey() >= 0) { // skip input/output anchors
                Node old = entry.getValue();
                float right = old.x + old.width;
                if (right > rightmostX) {
                    rightmostX = right;
                    lastBlockY = old.y;
                }
            }
        }

        for (int i = 0; i < count; i++) {
            String name = null;
            try { name = AudioEngine.getActivePluginName(i); } catch (Exception e) { Log.w("SignalChainCanvas", "name fail i=" + i, e); }
            if (name == null) name = "Plugin " + i;
            String displayName = PluginNaming.displayName(name);

            float cx = xCursor;
            float cy = yCenter - nodeH / 2f;
            xCursor += nodeW + spacing;

            Node n = new Node(i, AudioEngine.getActivePluginID(i), displayName, cx, cy);
            slotToPluginId.put(i, n.pluginId);
            n.categoryColor = getCategoryColor(name);
            n.categoryIconRes = getCategoryIconRes(name);
            n.categoryCode = getCategoryCode(name);
            n.width = nodeW;
            n.height = nodeH;

            // SPLIT / MERGE are structured routing blocks (listed in the Add-Block menu).
            String nlow = name.toLowerCase();
            if (nlow.contains("split")) {
                n.nodeType = NodeType.SPLIT;
                // R19 TEMUAN #2 (Hery): tile color = the Utility category color
                // (the old orange MERGE / distinct grays are gone).
                n.categoryColor = CategoryHelper.COLOR_ROUTING;
                n.categoryIconRes = R.drawable.ic_routing_split;
                n.name = "SPLIT";
                // R16: the tile badge derives from the raw name ("Giggin Gear
                // SPLIT" -> UTILITY category code); routing blocks get their
                // own short code so the canvas reads SPLIT/MERGE.
                n.categoryCode = "SPLIT";
            } else if (nlow.contains("merge")) {
                n.nodeType = NodeType.MERGE;
                n.categoryColor = CategoryHelper.COLOR_ROUTING;
                n.categoryIconRes = R.drawable.ic_routing_merge;
                n.name = "MERGE";
                n.categoryCode = "MERGE";
            }

            // Preserve selection AND position from previous layout, or place new block right after the last block.
            // Bypass LEDs always re-read the engine: preset restores toggle
            // natively, so a stale preserved flag would draw the wrong state.
            // Prefer the previous node in the SAME slot (stable identity when
            // nothing moved); fall back to the kth unused old node with the
            // same plugin id (reorder case). Never "last LV2 block wins".
            // R15 §4: SPLIT/MERGE instantiate from ONE native ggrouting module
            // -> the SAME pluginId for both, so the reuse queue collided (MERGE
            // polled SPLIT's old node) and stacked both tiles on one spot.
            // Routing blocks skip reuse entirely: always laid out at their
            // sequential slot position.
            Node old = null;
            if (!n.isRouting()) {
                java.util.ArrayDeque<Node> queue = oldByPluginId.get(n.pluginId);
                Node sameSlot = oldNodes.get(i);
                if (sameSlot != null && !sameSlot.isAnchor() && sameSlot.pluginId == n.pluginId) {
                    old = sameSlot;
                    if (queue != null) queue.remove(sameSlot);
                } else if (queue != null) {
                    old = queue.pollFirst();
                }
            }
            if (old != null) {
                n.selected = old.selected;
                // X is canonical slot geometry. Preserving old X after a
                // reorder is what caused blocks to stack and wires to cross.
                n.x = cx;
                n.y = old.y;
                n.bypassed = old.bypassed;
            } else if (n.isRouting()) {
                // Sequential slot position: the Node ctor already holds cx
                // (xCursor advanced above); do NOT rightmost-place these.
            } else if (!oldNodes.isEmpty()) {
                // Auto-place new effect block in empty canvas space immediately to the right of the last block
                n.x = rightmostX + spacing;
                n.y = lastBlockY;
                rightmostX = n.x + n.width;
            }
            // Bypass LEDs always re-read the engine: preset restores toggle
            // natively, so a stale preserved/default flag would draw the
            // wrong state (e.g. bypassed block shown lit after cold start).
            try { n.bypassed = !AudioEngine.getActivePluginEnabled(i); } catch (Exception ignore) {}

            nodes.add(n);
            nodeMap.put(n.id, n);
        }

        // 3) OUTPUT anchor: preserve custom position like plugin blocks,
        // otherwise every reload resets OUT 1744 -> 1630 (see log 19:57:05)
        float outX;
        if (phoneScroll) {
            // Phone: OUT sits after the last block so long chains h-scroll
            // instead of squeezing. Short chains pin OUT near the right
            // viewport edge so IN->OUT still spans the home view.
            float maxRight = inputNode.x + inputNode.width;
            for (Node n : nodes) {
                float r = n.x + n.width;
                if (r > maxRight) maxRight = r;
            }
            float vW = Math.max(viewportWPx(), viewW);
            outX = Math.max(vW - anchorDim - 48 * density, maxRight + spacing);
        } else {
            outX = viewW - anchorDim - 48 * density;
        }
        float outY = yCenter - anchorDim / 2f;
        if (oldNodes.containsKey(-200)) {
            outX = oldNodes.get(-200).x;
            outY = oldNodes.get(-200).y;
        }
        Node outputNode = new Node(-200, -1, "OUT", outX, outY);
        outputNode.nodeType = NodeType.OUTPUT;
        outputNode.categoryColor = COLOR_OUTPUT;
        outputNode.width = anchorDim;
        outputNode.height = anchorDim;
        nodes.add(outputNode);
        nodeMap.put(outputNode.id, outputNode);
        outputAnchor = outputNode;

        if (phoneScroll) {
            // Content width spans the full chain (viewport at minimum).
            float vW = Math.max(viewportWPx(), viewW);
            float maxR = outX + anchorDim;
            for (Node n : nodes) {
                float r = n.x + n.width;
                if (r > maxR) maxR = r;
            }
            contentWPx = Math.max(vW, maxR + 16 * density);
            // Clamp into scrollable content bounds (not the viewport).
            for (Node n : nodes) {
                if (n.nodeType == NodeType.INPUT) continue; // Keep INPUT exactly at left edge
                n.x = Math.max(inputNode.x + inputNode.width + spacing,
                        Math.min(n.x, Math.max(inputNode.x, contentWPx - n.width - 16 * density)));
                n.y = Math.max(0, Math.min(n.y, Math.max(0, contentHPx - n.height)));
            }
        } else {
            // Safety clamp: all nodes inside canvas bounds (fixes dangling off-canvas lines)
            float strictFabR = 105 * density; // Generous margin so it never overlaps the FAB
            if (getWidth() > 0 && getHeight() > 0) {
                for (Node n : nodes) {
                    if (n.nodeType == NodeType.INPUT) continue; // Keep INPUT exactly at left edge
                    n.x = Math.max(inputNode.x + inputNode.width + spacing, Math.min(n.x, getWidth() - n.width - strictFabR));
                    n.y = Math.max(0, Math.min(n.y, getHeight() - n.height));
                }
            }
        }

        // SINGLE SOURCE OF TRUTH: block order defines signal order.
        //   - A chain WITHOUT explicit SPLIT/MERGE is PURE SERIES: the wiring is
        //     regenerated deterministically from the ordered block list every
        //     rebuild. Stale/accidental edges (e.g. a star topology left over
        //     from attachAnchorEdges on a cold canvas) are discarded so the
        //     canvas can never show one topology while the engine runs another.
        //   - A chain WITH SPLIT/MERGE keeps its explicit branch wiring: restored
        //     preset edges are honoured, and if none are present the deterministic
        //     auto-wire regenerates the identical branch topology from order.
        boolean hasSplit = false;
        boolean hasMerge = false;
        for (Node n : nodes) {
            if (n == null) continue;
            if (n.nodeType == NodeType.SPLIT) hasSplit = true;
            if (n.nodeType == NodeType.MERGE) hasMerge = true;
        }
        boolean hasRoutingBlock = hasSplit && hasMerge;

        if (!hasRoutingBlock) {
            // Series topology has one immutable visual row. Persisted/custom
            // Y coordinates are layout artifacts, never signal topology.
            for (Node n : nodes) {
                if (n == null || n.isAnchor()) continue;
                n.y = yCenter - n.height / 2f;
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

        // Wires are locked and declarative. Never restore slot-based edges:
        // after a reorder their ids refer to different plugins and produce
        // crossed/stacked lines. SPLIT/MERGE is the only thing that enables
        // parallel topology; its branch edges are derived from current order.
        edges.clear();
        java.util.List<Node> walk = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Node wn = nodeMap.get(i);
            if (wn != null) walk.add(wn);
        }
        autoWireRespectingRouting(inputNode, outputNode, walk);

        logOrderedChain(count);

        // Branch labels are always recomputed from block order so the two-row
        // layout stays correct even when edges were restored from a preset.
        assignBranchLabels(walk);

        // R19: parallel rigs use the columnar two-row layout — lane A blocks
        // in the upper row, lane B in the lower, each A_i/B_i pair sharing
        // one x column ("sejajar", no diagonal stagger). Series chains stay
        // on the single trunk row.
        if (hasRoutingBlock) {
            layoutRoutingSlots(walk, yCenter);
        } else {
            layoutSeriesSlots();
        }

        syncGraphToAudioEngine();

        requestLayout();
        invalidate();
    
        // Apply pending custom node positions (from preset load) after rebuild,
        // so slow engine adds can't outrun the restore (S23 timing bug).
        if (pendingCustomPos != null && !pendingCustomPos.isEmpty()) {
            setCustomNodePositions(pendingCustomPos);
            pendingCustomPos = null;
        }

        // R19: when a block (incl. SPLIT/MERGE) is ADDED, scroll the phone
        // canvas so the new tail block is in view. Only fires on count
        // growth — first layout and same-count rebuilds never scroll.
        if (phoneScroll && prevNodeCount >= 0 && nodes.size() > prevNodeCount) {
            post(new Runnable() {
                @Override public void run() {
                    Node last = null;
                    for (Node n : nodes) if (!n.isAnchor()) last = n;
                    if (last == null) return;
                    android.view.ViewParent p = getParent();
                    while (p != null && !(p instanceof android.widget.HorizontalScrollView)) {
                        p = p.getParent();
                    }
                    if (p instanceof android.widget.HorizontalScrollView) {
                        android.widget.HorizontalScrollView hsv =
                                (android.widget.HorizontalScrollView) p;
                        int target = Math.round(last.x + last.width / 2f - getWidth() / 2f);
                        hsv.smoothScrollTo(Math.max(0, target), hsv.getScrollY());
                    }
                }
            });
        }
        prevNodeCount = nodes.size();
        StringBuilder sbAfter = new StringBuilder();
        for (Node n : nodes) {
            sbAfter.append(n.id).append("/").append(n.name).append("/").append(n.x).append("/").append(n.y).append(";sel=").append(n.selected).append(";");
        }
        // Log selected plugin
        Node selected = null;
        for (Node n : nodes) if (n.selected) { selected = n; break; }
        if (selected != null) {
        } else {
        }
    }

    /**
     * Walk blocks in chain order and tag each with its branch row:
     * 0 = trunk, 1 = upper parallel branch, 2 = lower parallel branch.
     * Derived purely from SPLIT/MERGE positions, so it is stable across
     * rebuilds and preset restores.
     */
    private void assignBranchLabels(java.util.List<Node> walk) {
        boolean inSplit = false;
        boolean nextUpper = true;
        for (Node n : walk) {
            if (n == null) continue;
            if (n.nodeType == NodeType.SPLIT) {
                n.branch = 0;
                inSplit = true;
                nextUpper = true;
            } else if (n.nodeType == NodeType.MERGE) {
                n.branch = 0;
                inSplit = false;
            } else if (inSplit) {
                n.branch = nextUpper ? 1 : 2;
                nextUpper = !nextUpper;
            } else {
                n.branch = 0;
            }
        }
    }

    /**
     * Declarative series/parallel auto-wiring driven by inserted SPLIT/MERGE blocks
     * (primary interaction model — no port-dot dragging required).
     *
     * Rules, walking the chain left to right:
     *   - Normal blocks chain straight through (plain series line).
     *   - A SPLIT forks the signal: every block after it alternates into the upper
     *     branch then the lower branch, so each branch is populated independently.
     *   - A MERGE recombines: both branch tails feed into it, and the chain
     *     continues as a single row afterwards.
     */
    private void autoWireRespectingRouting(Node inputNode, Node outputNode, java.util.List<Node> walk) {
        edges.addAll(deriveWiringEdges(inputNode, outputNode, walk));
    }

    /** Pure edge derivation (package-private for RoutingWiresTest). The
     *  algorithm is the exact pre-R19 wiring: the one-row layout change only
     *  moves geometry, never topology — this test pins that invariant. */
    static java.util.List<Edge> deriveWiringEdges(Node inputNode, Node outputNode,
                                                  java.util.List<Node> walk) {
        java.util.List<Edge> out = new java.util.ArrayList<>();
        boolean hasSplit = false;
        boolean hasMerge = false;
        for (Node n : walk) {
            if (n == null) continue;
            if (n.nodeType == NodeType.SPLIT) hasSplit = true;
            if (n.nodeType == NodeType.MERGE) hasMerge = true;
        }
        // A lone routing block is incomplete and must not create a partial
        // parallel graph. It remains a normal series node until its matching
        // SPLIT/MERGE counterpart is present.
        if (!hasSplit || !hasMerge) {
            int tail = inputNode.id;
            for (Node n : walk) {
                if (n == null) continue;
                out.add(new Edge(tail, n.id));
                tail = n.id;
                n.branch = 0;
            }
            out.add(new Edge(tail, outputNode.id));
            return out;
        }
        int trunkTail = inputNode.id;   // last node on the single (non-split) path
        int upperTail = -1;             // last node on upper branch (-1 = none yet)
        int lowerTail = -1;             // last node on lower branch
        boolean inSplit = false;
        boolean nextGoesUpper = true;

        for (Node n : walk) {
            if (n == null) continue;

            if (n.nodeType == NodeType.SPLIT) {
                // SPLIT consumes the current trunk and opens two branches.
                out.add(new Edge(trunkTail, n.id));
                inSplit = true;
                upperTail = n.id;
                lowerTail = n.id;
                nextGoesUpper = true;
                n.branch = 0;
                continue;
            }

            if (n.nodeType == NodeType.MERGE) {
                // MERGE recombines both branch tails (2 inputs) into one trunk.
                if (inSplit) {
                    out.add(new Edge(upperTail, n.id));
                    if (lowerTail != upperTail) out.add(new Edge(lowerTail, n.id));
                } else {
                    out.add(new Edge(trunkTail, n.id));
                }
                inSplit = false;
                trunkTail = n.id;
                upperTail = -1;
                lowerTail = -1;
                n.branch = 0;
                continue;
            }

            if (inSplit) {
                // Alternate effect blocks between the two branches so both populate.
                if (nextGoesUpper) {
                    out.add(new Edge(upperTail, n.id));
                    upperTail = n.id;
                    n.branch = 1;
                } else {
                    out.add(new Edge(lowerTail, n.id));
                    lowerTail = n.id;
                    n.branch = 2;
                }
                nextGoesUpper = !nextGoesUpper;
            } else {
                out.add(new Edge(trunkTail, n.id));
                trunkTail = n.id;
                n.branch = 0;
            }
        }

        // Terminate: any dangling branch tails also reach OUTPUT (implicit sum).
        if (inSplit) {
            out.add(new Edge(upperTail, outputNode.id));
            if (lowerTail != upperTail) out.add(new Edge(lowerTail, outputNode.id));
        } else {
            out.add(new Edge(trunkTail, outputNode.id));
        }
        return out;
    }

    /** Keep every block in a deterministic non-overlapping slot after a drop.
     *  Wires remain derived from this order; user movement never edits edges.
     *  @param explicitOrderIds engine slot ids in the NEW left-to-right order
     *         (drag drop), or null to lay out in the current nodes list order
     *         (post-rebuild: list order == engine order). */
    private void layoutSeriesSlots(java.util.List<Integer> explicitOrderIds) {
        float d = density();
        float spacing = 12f * d;
        float x = inputAnchor != null ? inputAnchor.x + inputAnchor.width + spacing : 8f * d;
        float y = phoneScroll && contentHPx > 0 ? contentHPx / 2f
                : (getHeight() > 0 ? getHeight() / 2f : 48f * d);
        float maxRight = x;
        java.util.List<Node> ordered = new java.util.ArrayList<>();
        if (explicitOrderIds != null) {
            for (Integer id : explicitOrderIds) {
                Node n = nodeMap.get(id);
                if (n != null && !n.isAnchor()) ordered.add(n);
            }
        } else {
            for (Node n : nodes) {
                if (n == null || n.isAnchor()) continue;
                ordered.add(n);
            }
        }
        for (Node n : ordered) {
            n.x = x;
            n.y = Math.max(0f, y - n.height / 2f);
            x += n.width + spacing;
            maxRight = n.x + n.width;
        }
        if (outputAnchor != null) {
            outputAnchor.x = Math.max(x, outputAnchor.x);
            outputAnchor.y = Math.max(0f, y - outputAnchor.height / 2f);
            maxRight = outputAnchor.x + outputAnchor.width;
        }
        if (phoneScroll) {
            contentWPx = Math.max(contentWPx, maxRight + 16f * d);
        }
    }

    private void layoutSeriesSlots() {
        layoutSeriesSlots(null);
    }

    /** R19 columnar two-row parallel layout (r18 complaint: lane A/B zigzag
     *  diagonally because x followed chain order interleaved A,B,A,B).
     *  Lane A blocks go in the upper row, lane B in the lower row, and each
     *  A_i/B_i pair shares ONE x column ("sejajar") — top and bottom blocks
     *  line up vertically. Trunk prefix/suffix stay on the center baseline;
     *  SPLIT/MERGE sit on the trunk between the columns.
     *  @param walk chain-order nodes (branch labels already assigned)
     *  @param yCenter trunk baseline (center row) */
    private void layoutRoutingSlots(java.util.List<Node> walk, float yCenter) {
        float d = density();
        float spacing = 12f * d;
        float x = inputAnchor != null ? inputAnchor.x + inputAnchor.width + spacing : 8f * d;
        float maxH = 0f;
        for (Node n : walk) {
            if (n != null && !n.isAnchor() && n.height > maxH) maxH = n.height;
        }
        float offset = (maxH * 0.62f) + (spacing * 0.35f);

        java.util.List<Node> laneA = new java.util.ArrayList<>();
        java.util.List<Node> laneB = new java.util.ArrayList<>();
        boolean inSplit = false;

        for (Node n : walk) {
            if (n == null || n.isAnchor()) continue;

            if (n.nodeType == NodeType.SPLIT) {
                n.x = x;
                n.y = Math.max(0f, yCenter - n.height / 2f);
                x += n.width + spacing;
                laneA.clear();
                laneB.clear();
                inSplit = true;
                continue;
            }

            if (n.nodeType == NodeType.MERGE) {
                x = layOutLaneColumns(laneA, laneB, x, yCenter, offset);
                n.x = x;
                n.y = Math.max(0f, yCenter - n.height / 2f);
                x += n.width + spacing;
                laneA.clear();
                laneB.clear();
                inSplit = false;
                continue;
            }

            if (inSplit && (n.branch == 1 || n.branch == 2)) {
                if (n.branch == 1) laneA.add(n);
                else laneB.add(n);
            } else {
                // Trunk (before SPLIT / after MERGE, or lone-routing fallback).
                n.x = x;
                n.y = Math.max(0f, yCenter - n.height / 2f);
                x += n.width + spacing;
            }
        }
        // Dangling lanes (SPLIT without its MERGE mid-rebuild): park them on
        // the center row after everything else so nothing is left at old x.
        x = layOutLaneColumns(laneA, laneB, x, yCenter, 0f);

        float maxRight = x;
        if (outputAnchor != null) {
            outputAnchor.x = Math.max(x, outputAnchor.x);
            outputAnchor.y = Math.max(0f, yCenter - outputAnchor.height / 2f);
            maxRight = outputAnchor.x + outputAnchor.width;
        }
        if (phoneScroll) {
            contentWPx = Math.max(contentWPx, maxRight + 16f * d);
        }
    }

    /** Place paired lane columns: A_i upper row, B_i lower row, same x.
     *  @return x cursor after the last column */
    private float layOutLaneColumns(java.util.List<Node> laneA, java.util.List<Node> laneB,
                                    float x, float yCenter, float offset) {
        float d = density();
        float spacing = 12f * d;
        int cols = Math.max(laneA.size(), laneB.size());
        for (int i = 0; i < cols; i++) {
            float colW = 0f;
            if (i < laneA.size()) colW = Math.max(colW, laneA.get(i).width);
            if (i < laneB.size()) colW = Math.max(colW, laneB.get(i).width);
            if (i < laneA.size()) {
                Node a = laneA.get(i);
                a.x = x;
                // offset == 0 (dangling fallback): park on the center row.
                a.y = Math.max(0f, offset == 0f ? yCenter - a.height / 2f
                        : yCenter - a.height / 2f - offset);
            }
            if (i < laneB.size()) {
                Node b = laneB.get(i);
                b.x = x;
                b.y = Math.max(0f, offset == 0f ? yCenter - b.height / 2f
                        : yCenter - b.height / 2f + offset);
            }
            x += colW + spacing;
        }
        return x;
    }

    private void syncGraphToAudioEngine() {
        // Strip out INPUT (-100) and OUTPUT (-200) anchors.
        // The native GraphSolver only accepts valid plugin indices (0..N-1).
        // Passing negative IDs causes the explicit graph to be rejected and falls back to linear series!
        java.util.List<Edge> activeEdges = new java.util.ArrayList<>();
        for (Edge e : edges) {
            if (e.fromNodeId >= 0 && e.toNodeId >= 0) {
                activeEdges.add(e);
            }
        }
        
        int[] from = new int[activeEdges.size()];
        int[] to = new int[activeEdges.size()];
        for (int i = 0; i < activeEdges.size(); i++) {
            from[i] = activeEdges.get(i).fromNodeId;
            to[i] = activeEdges.get(i).toNodeId;
        }
        try {
            AudioEngine.setGraph(from, to);
        } catch (Exception e) {
            Log.e("SignalChainCanvas", "setGraph failed", e);
        }
    }

    /** TEMPORARY verification: canonical ordered chain (block order == signal
     *  order) as seen by the UI, so canvas order and engine [PLAN] can be
     *  compared in one logcat capture. */
    private void logOrderedChain(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            Node n = nodeMap.get(i);
            if (n == null) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(i).append(':').append(n.name);
        }
        Log.i("SignalChainCanvas", "[CHAIN] ordered=" + sb);
    }

    public void setGraphEdges(int[] from, int[] to) {
        edges.clear();
        // Normalization: a preset whose chain has NO SPLIT/MERGE must load as
        // PURE SERIES. Legacy presets can carry an accidental parallel wire
        // graph left over from an older canvas; restoring it would keep the
        // "canvas parallel, engine series" mismatch alive. With no routing
        // block, order-driven auto-wiring rebuilds the single series path.
        boolean hasSplit = false;
        boolean hasMerge = false;
        for (Node n : nodes) {
            if (n == null) continue;
            if (n.nodeType == NodeType.SPLIT) hasSplit = true;
            if (n.nodeType == NodeType.MERGE) hasMerge = true;
        }
        boolean hasRoutingBlock = hasSplit && hasMerge;
        if (inputAnchor != null && outputAnchor != null) {
            // Ignore persisted edges. They are slot-indexed and therefore stale
            // after any block move; the current block order is authoritative.
            java.util.List<Node> walk = new java.util.ArrayList<>();
            for (Node n : nodes) if (n != null && !n.isAnchor()) walk.add(n);
            autoWireRespectingRouting(inputAnchor, outputAnchor, walk);
        }
        // Push the normalized wiring to the engine: a preset's SPLIT/MERGE
        // branch topology is authoritative; a series preset sends order-derived
        // series edges (or none, which the engine also reads as ordered series).
        syncGraphToAudioEngine();
        invalidate();
    }

    /** Feed every head node from INPUT and drain every tail node to OUTPUT —
     *  the same invariant autoWireRespectingRouting() uses, applied to
     *  explicitly restored edges. Idempotent (checks existing endpoints). */
    private void attachAnchorEdges() {
        java.util.Set<Integer> hasIncoming = new java.util.HashSet<>();
        java.util.Set<Integer> hasOutgoing = new java.util.HashSet<>();
        for (Edge e : edges) {
            hasIncoming.add(e.toNodeId);
            hasOutgoing.add(e.fromNodeId);
        }
        boolean hasPluginNode = false;
        for (Node n : nodes) {
            if (n == null || n.id < 0) continue;
            hasPluginNode = true;
            if (!hasIncoming.contains(n.id)) edges.add(new Edge(-100, n.id));
            if (!hasOutgoing.contains(n.id)) edges.add(new Edge(n.id, -200));
        }
        if (!hasPluginNode && edges.isEmpty()) edges.add(new Edge(-100, -200));
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
            Node n = nodes.get(i);
            // Anchors (INPUT/OUTPUT) and routing blocks (SPLIT/MERGE) are all selectable
            if (n.contains(tx, ty)) return n;
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
                if (hit != null) {
                    // Tap di dalam block = SELECT + siap drag (bukan port)
                    long now = SystemClock.elapsedRealtime();
                    if (lastTapNodeId == hit.id && now - lastTapTime < 320L && !hit.isAnchor()) {
                        // Double tap = bypass on/off
                        try {
                            boolean en = AudioEngine.getActivePluginEnabled(hit.id);
                            AudioEngine.togglePlugin(hit.id, !en);
                            hit.bypassed = en;
                            toastBypass(hit, !en);
                            invalidate();
                        } catch (Exception ex) {
                            Log.w("SignalChainCanvas", "bypass toggle failed", ex);
                        }
                        lastTapTime = 0; lastTapNodeId = -1;
                    } else {
                        lastTapTime = now; lastTapNodeId = hit.id;
                    }
                    for (Node n : nodes) n.selected = false;
                    hit.selected = true;
                    lastX = tx;
                    lastY = ty;
                    dragFromIndex = hit.id;
                    dragStarted = false;
                    if (listener != null) listener.onNodeSelected(hit);
                    dragNode = hit;
                    dragWirePositions.clear();
                    for (Edge e : edges) {
                        Node a = nodeMap.get(e.fromNodeId);
                        Node b = nodeMap.get(e.toNodeId);
                        if (a != null) {
                            dragWirePositions.put(a.id, new float[]{a.getOutputX(), a.getOutputY()});
                        }
                        if (b != null) {
                            dragWirePositions.put(b.id, new float[]{b.getInputX(), b.getInputY()});
                        }
                    }
                    if (phoneScroll) {
                        // Block drag owns this gesture: stop the scroll
                        // parents from stealing it. Empty-space touches keep
                        // scrolling (the fallthrough below stays interceptible).
                        try { getParent().requestDisallowInterceptTouchEvent(true); } catch (Exception ignore) {}
                    }
                    invalidate();
                    return true;
                }

                // NOTE (topology): connection lines are AUTO-GENERATED from block
                // order + explicit SPLIT/MERGE only. Manual wire drawing and
                // wire deletion are disabled — no port-drag, no wire tap.
                return true;
                case MotionEvent.ACTION_MOVE:
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
                        // Clamp: don't cross input node, don't overlap "+" button.
                        // Phone clamps into scrollable content bounds; tablet
                        // keeps the viewport clamp.
                        float boundW = (phoneScroll && contentWPx > 0) ? contentWPx : getWidth();
                        float boundH = (phoneScroll && contentHPx > 0) ? contentHPx : getHeight();
                        float fabR = 105f * getResources().getDisplayMetrics().density;
                        float minX = (inputAnchor != null) ? (inputAnchor.x + inputAnchor.width + 12f * getResources().getDisplayMetrics().density) : 0;
                        nx = Math.max(minX, Math.min(nx, boundW - dragNode.width - fabR));
                        ny = Math.max(0, Math.min(ny, boundH - dragNode.height));
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
                if (phoneScroll) {
                    try { getParent().requestDisallowInterceptTouchEvent(false); } catch (Exception ignore) {}
                }
                // Manual wire creation removed: lines are auto-generated from
                // block order + explicit SPLIT/MERGE (see NOTE in ACTION_DOWN).
                // Auto-justify snap on DROP
                 if (dragNode != null && !dragNode.isAnchor() && dragStarted) {
                     float density = getResources().getDisplayMetrics().density;
                     float rowHeight = (dragNode.height > 0 ? dragNode.height : 62 * density) + 10 * density;
                     float yCenter = getHeight() / 2f - dragNode.height / 2f;
                     int row = Math.round((dragNode.y - yCenter) / rowHeight);
                     dragNode.y = yCenter + (row * rowHeight);
                     // Clamp snap ke dalam bounds canvas (phone: content bounds)
                    float snapW = (phoneScroll && contentWPx > 0) ? contentWPx : getWidth();
                    float snapH = (phoneScroll && contentHPx > 0) ? contentHPx : getHeight();
                    // NOTE: vertical position is LAYOUT ONLY. It never creates a
                    // branch — parallel routing exists solely through explicit
                    // SPLIT/MERGE blocks (Add-Block menu). Dropping a block off
                    // the series row therefore just moves it; topology is
                    // unchanged.
                    dragNode.x = Math.max(0, Math.min(dragNode.x, snapW - dragNode.width));
                    dragNode.y = Math.max(0, Math.min(dragNode.y, snapH - dragNode.height));
                     // Build the new left-to-right order. Zone-aware: the
                     // pre-drag chain (engine slot order) assigns every block
                     // a zone — trunk stretch, the SPLIT that opens a pair, the
                     // branch content between SPLIT/MERGE, the MERGE, the next
                     // trunk stretch. x only competes inside the same zone, so
                     // a branch block can NEVER be dropped past its MERGE just
                     // because the finger drifted vertically (r18 R4.1: a 25px
                     // nudge ejected DELAY out of the branch). A purely
                     // vertical move therefore no-ops: zones unchanged, x
                     // unchanged -> identical order.
                     java.util.List<Node> pre = new java.util.ArrayList<>();
                     for (Node n : nodes) {
                         if (n == null || n.isAnchor()) continue;
                         pre.add(n);
                     }
                     java.util.Collections.sort(pre, (a, b) -> Integer.compare(a.id, b.id));
                     // R20 (r19 R2.5): columnar layout gives lane A_i and B_i the
                     // SAME x, so a cross-lane drop onto the other row ties on
                     // (zone, x) and the stable sort restores the old chain —
                     // A<->B swaps were impossible. A drop that lands ON another
                     // block's slot (x and y both match after the row snap
                     // above) is a direct slot swap: exchange the two chain
                     // positions up front. The later stable sort keeps the swap
                     // because every key ties. Purely vertical drags land in
                     // empty slots (rows differ), so they stay no-ops.
                     final float slotEps = 6f * density;
                     for (Node n : pre) {
                         if (n == dragNode || n.isAnchor()) continue;
                         if (Math.abs(n.x - dragNode.x) < slotEps
                                 && Math.abs(n.y - dragNode.y) < slotEps) {
                             int pi = pre.indexOf(dragNode);
                             int ni = pre.indexOf(n);
                             if (pi >= 0 && ni >= 0) {
                                 pre.set(pi, n);
                                 pre.set(ni, dragNode);
                             }
                             break;
                         }
                     }
                     java.util.Map<Integer, Integer> zoneOf = new java.util.HashMap<>();
                     int z = 0, sharedTrunk = 0, branchZone = -1;
                     boolean inPair = false;
                     for (Node n : pre) {
                         if (n.nodeType == NodeType.SPLIT) {
                             z++;
                             zoneOf.put(n.id, z);
                             z++;
                             branchZone = z;
                             inPair = true;
                         } else if (n.nodeType == NodeType.MERGE) {
                             z++;
                             zoneOf.put(n.id, z);
                             z++;
                             sharedTrunk = z;
                             inPair = false;
                             branchZone = -1;
                         } else if (inPair) {
                             zoneOf.put(n.id, branchZone);
                         } else {
                             zoneOf.put(n.id, sharedTrunk);
                         }
                     }
                     zoneOf.putIfAbsent(dragNode.id, sharedTrunk);

                     java.util.List<Node> allOrdered = new java.util.ArrayList<>(pre);
                     allOrdered.sort((a, b) -> {
                         int za = zoneOf.getOrDefault(a.id, 0);
                         int zb = zoneOf.getOrDefault(b.id, 0);
                         if (za != zb) return Integer.compare(za, zb);
                         int cx = Float.compare(a.x, b.x);
                         if (cx != 0) return cx;
                         // Same column (columnar lanes): upper row (smaller y)
                         // sorts first — lets a lane-crossing drop keep its new
                         // row when x ties.
                         return Float.compare(a.y, b.y);
                     });

                     java.util.List<Integer> order = new java.util.ArrayList<>();
                     for (Node n : allOrdered) order.add(n.id);

                     int[] newOrder = new int[order.size()];
                     boolean orderChanged = false;
                     for (int k = 0; k < order.size(); k++) {
                         newOrder[k] = order.get(k);
                         if (newOrder[k] != k) orderChanged = true;
                     }
                      // Reflow the NEW order using canonical slot geometry (same
                      // math as the rebuild): re-snapping the dropped block to
                      // 8dp + col*colWidth collided with the INPUT anchor and its
                      // neighbour, stacking the drop on slot 0.
                      // R19: columnar two-row — layoutRoutingSlots() owns all
                      // geometry for SPLIT/MERGE chains; drop just re-derives
                      // wires + labels, then re-columns the lanes.
                      layoutSeriesSlots(order);
                      java.util.List<Node> walk = new java.util.ArrayList<>();
                      for (Integer id : order) {
                          Node wn = nodeMap.get(id);
                          if (wn != null) walk.add(wn);
                      }
                      boolean walkHasRouting = false;
                      for (Node wn : walk) {
                          if (wn.nodeType == NodeType.SPLIT || wn.nodeType == NodeType.MERGE) {
                              walkHasRouting = true;
                              break;
                          }
                      }
                      if (walkHasRouting && inputAnchor != null && outputAnchor != null) {
                          edges.clear();
                          autoWireRespectingRouting(inputAnchor, outputAnchor, walk);
                          assignBranchLabels(walk);
                          float dropYCenter = phoneScroll ? contentHPx / 2f
                                  : (getHeight() > 0 ? getHeight() / 2f : 48f * density());
                          layoutRoutingSlots(walk, dropYCenter);
                      }
                     if (orderChanged && reorderListener != null) {
                         Log.i("SignalChainCanvas", "[DROP] commit order=" + order
                                 + " drag=" + dragNode.name + " @x=" + dragNode.x);
                         reorderListener.onReorder(newOrder);
                     } else {
                         Log.i("SignalChainCanvas", "[DROP] no-op order=" + order
                                 + " drag=" + dragNode.name + " @x=" + dragNode.x);
                         syncGraphToAudioEngine();
                     }
                }

                dragNode = null;
                dragWirePositions.clear();
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

    public void setPendingCustomPositions(java.util.Map<String, float[]> p) {
        this.pendingCustomPos = p;
    }

    public Map<String, float[]> captureNodePositions() {
        Map<String, float[]> map = new HashMap<>();
        for (Node n : nodes) {
            map.put(String.valueOf(n.id), new float[]{n.x, n.y});
        }
        return map;
    }

    private void toastBypass(Node n, boolean enabled) {
        try {
            android.widget.Toast.makeText(getContext(),
                n.name + (enabled ? " ON" : " BYPASSED"),
                android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception ignore) {}
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
                    if (phoneScroll) {
                        // Restored (possibly cross-form-factor) positions must
                        // stay inside the scrollable content; grow the content
                        // to fit instead of stranding blocks off-scroll.
                        n.x = Math.max(0, n.x);
                        n.y = Math.max(0, n.y);
                        contentWPx = Math.max(contentWPx, n.x + n.width + 16 * density());
                        contentHPx = Math.max(contentHPx, n.y + n.height + 16 * density());
                    }
                }
            }
        }
        // De-overlap: restored custom positions may collide (two blocks at the
        // same x in the same row). Push the later block right so each keeps
        // its own slot; parallel branch rows (different y) are untouched.
        java.util.List<Node> rowNodes = new java.util.ArrayList<>();
        for (Node n : nodes) {
            if (n != null && !n.isAnchor()) rowNodes.add(n);
        }
        java.util.Collections.sort(rowNodes, (a, b) -> Float.compare(a.x, b.x));
        float minGap = 4 * density();
        for (int i = 1; i < rowNodes.size(); i++) {
            Node prev = rowNodes.get(i - 1);
            Node cur = rowNodes.get(i);
            if (Math.abs(cur.y - prev.y) > (cur.height + prev.height) * 0.25f) continue;
            float minNextX = prev.x + prev.width + minGap;
            if (cur.x < minNextX) cur.x = minNextX;
        }
        if (phoneScroll) requestLayout(); // re-measure if content grew above
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (phoneScroll) {
            // Phone: inside the scroll parents the specs are UNSPECIFIED, so
            // measure to the full content size. EXACT specs (fillViewport
            // stretch when content is smaller) are honored as-is. Tablet
            // never reaches here (flag false in values-sw600dp).
            int wMode = MeasureSpec.getMode(widthSpec);
            int hMode = MeasureSpec.getMode(heightSpec);
            float d = density();
            int w = (wMode == MeasureSpec.UNSPECIFIED)
                    ? Math.round(contentWPx > 0 ? contentWPx : 380 * d)
                    : MeasureSpec.getSize(widthSpec);
            int h = (hMode == MeasureSpec.UNSPECIFIED)
                    ? Math.round(contentHPx > 0 ? contentHPx : viewportHPx())
                    : MeasureSpec.getSize(heightSpec);
            if (w <= 0) w = Math.round(380 * d);
            if (h <= 0) h = Math.round(viewportHPx());
            setMeasuredDimension(w, h);
            return;
        }
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            float density = getResources().getDisplayMetrics().density;
            float anchorDim = 24 * density;
            float yCenter = h / 2f - anchorDim / 2f;
            float fabReserved = 48 * density;

            if (inputAnchor != null) {
                inputAnchor.x = 4 * density;
                inputAnchor.y = yCenter;
            }
            if (outputAnchor != null) {
                // Phone: OUT.x lives in scrollable content coords (set at
                // load); only re-center vertically. Tablet path unchanged.
                if (!phoneScroll) {
                    outputAnchor.x = w - anchorDim - fabReserved;
                }
                outputAnchor.y = yCenter;
            }
            invalidate();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float anchorDim = 24 * density;
        float yCenter = getHeight() / 2f - anchorDim / 2f;
        float fabReserved = 48 * density;

        if (inputAnchor != null) {
            inputAnchor.x = 4 * density;
            inputAnchor.y = yCenter;
        }
        if (outputAnchor != null && getWidth() > 0) {
            // Phone: OUT.x lives in scrollable content coords (set at load);
            // only re-center vertically. Tablet path unchanged.
            if (!phoneScroll) {
                outputAnchor.x = getWidth() - anchorDim - fabReserved;
            }
            outputAnchor.y = yCenter;
        }

        // Phone drag-to-parallel affordance: while a block is being dragged in
        // a series-only chain, hint at the upper/lower drop rows with dashed
        // guides. Purely visual — routing still comes from SPLIT/MERGE blocks.
        if (phoneScroll && !lastHadParallel && dragNode != null && dragStarted
                && contentRowPitchPx > 0 && getWidth() > 0) {
            float cy = getHeight() / 2f;
            Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
            guide.setColor(Color.argb(90, 255, 138, 0));
            guide.setStyle(Paint.Style.STROKE);
            guide.setStrokeWidth(2f * density);
            guide.setPathEffect(new android.graphics.DashPathEffect(
                    new float[]{10f * density, 8f * density}, 0));
            float gw = Math.max(getWidth(), contentWPx);
            for (int s = -1; s <= 1; s += 2) {
                float gy = cy + s * contentRowPitchPx;
                canvas.drawLine(0, gy, gw, gy, guide);
            }
        }

        for (Edge e : edges) {
            Node a = nodeMap.get(e.fromNodeId);
            Node b = nodeMap.get(e.toNodeId);
            if (a == null || b == null) continue;
            // R17 §2.3: a segment inside a parallel branch wears that branch's
            // color. Source branch wins when set (block -> next block); the
            // SPLIT -> first-branch-block wire inherits the destination branch.
            int wireBranch = a.branch != 0 ? a.branch : b.branch;
            int wireColor = a.isAnchor() ? WIRE_COLOR
                    : (branchAccentColor(wireBranch) != 0 ? branchAccentColor(wireBranch)
                                                          : a.categoryColor);
            float ax = a.getOutputX();
            float ay = a.getOutputY();
            float bx = b.getInputX();
            float by = b.getInputY();
            if (dragNode != null && dragStarted && !dragWirePositions.isEmpty()) {
                float[] ap = dragWirePositions.get(a.id);
                float[] bp = dragWirePositions.get(b.id);
                if (ap != null) { ax = ap[0]; ay = ap[1]; }
                if (bp != null) { bx = bp[0]; by = bp[1]; }
            }
            drawBezierWire(canvas, ax, ay, bx, by, wireColor);
        }

        // (Manual temp-wire preview removed with manual wire drawing.)

        // Draw nodes and anchors
        for (Node n : nodes) {
            if (n.isAnchor()) {
                drawAnchorNode(canvas, n);
            } else {
                drawNode(canvas, n);
            }
        }

        drawRoutingLegend(canvas);
    }

    /**
     * R17 §2.3: routing legend while any block sits in a parallel branch.
     * Fixed to the viewport corner (survives canvas scrolling) so the branch
     * colors are always explained: A = blue, B = amber.
     */
    private void drawRoutingLegend(Canvas canvas) {
        boolean anyBranch = false;
        for (Node n : nodes) {
            if (!n.isAnchor() && n.branch != 0) { anyBranch = true; break; }
        }
        if (!anyBranch) return;

        float d = getResources().getDisplayMetrics().density;
        float pad = 6f * d;
        float chipH = 16f * d;
        float dotR = 3.5f * d;
        float gap = 5f * d;
        float between = 10f * d;
        float textSize = 8.5f * d;

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(textSize);
        labelPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.LEFT);

        float entryW = dotR * 2 + gap + labelPaint.measureText("A");
        float legendW = pad * 2 + entryW * 2 + between;
        float x0 = 8f * d, y0 = 8f * d;
        android.graphics.RectF r = new android.graphics.RectF(x0, y0, x0 + legendW, y0 + chipH + pad * 2);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.parseColor("#CC161A1F"));
        canvas.drawRoundRect(r, 6f * d, 6f * d, bg);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        border.setColor(Color.parseColor("#2A2F37"));
        canvas.drawRoundRect(r, 6f * d, 6f * d, border);

        float cx = x0 + pad;
        float cy = r.centerY();
        drawLegendEntry(canvas, cx, cy, dotR, gap, textSize, COLOR_BRANCH_A, "A", labelPaint);
        cx += entryW + between;
        drawLegendEntry(canvas, cx, cy, dotR, gap, textSize, COLOR_BRANCH_B, "B", labelPaint);
    }

    private void drawLegendEntry(Canvas canvas, float x, float cy, float dotR, float gap,
                                 float textSize, int color, String label, Paint labelPaint) {
        float d = getResources().getDisplayMetrics().density;
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(color);
        canvas.drawCircle(x + dotR, cy, dotR, dot);
        labelPaint.setColor(color);
        labelPaint.setTextSize(textSize);
        android.graphics.Paint.FontMetrics fm = labelPaint.getFontMetrics();
        canvas.drawText(label, x + dotR * 2 + gap, cy - (fm.ascent + fm.descent) / 2f, labelPaint);
    }

    private void drawAnchorNode(Canvas canvas, Node n) {
        float cx = n.x + n.width / 2f;
        float cy = n.y + n.height / 2f;
        float radius = n.width / 2f;
        float d = getResources().getDisplayMetrics().density;

        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
         // IN/OUT are connection labels, not effect blocks: text only, no
         // background circle or border.
         Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
         textPaint.setColor(Color.WHITE);
         textPaint.setTextAlign(Paint.Align.CENTER);
         textPaint.setTextSize(10f * d);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(n.name.toUpperCase(), cx, textY, textPaint);
    }

    private int dimColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] *= factor;
        hsv[2] = Math.min(hsv[2] * 1.1f, 1.0f);
        return Color.HSVToColor(200, hsv);
    }

    private void drawNode(Canvas canvas, Node n) {
        RectF r = new RectF(n.x, n.y, n.x + n.width, n.y + n.height);

        paintNodeBody.setColor(Color.parseColor("#1F2228"));
        canvas.drawRoundRect(r, 12, 12, paintNodeBody);

        // Bypassed nodes use dimmed/desaturated category color
        int drawColor = n.bypassed ? dimColor(n.categoryColor, 0.35f) : n.categoryColor;
        // R17 §2.3: blocks inside a parallel branch wear the branch color on
        // their border + ports; the icon keeps the category color. 0 = trunk.
        int branchAccent = branchAccentColor(n.branch);

        if (n.selected) {
            // Selected glow follows the branch accent inside parallel
            // branches (same rule as the unselected border below);
            // trunk blocks keep the category color.
            int glowColor = branchAccent != 0 ? branchAccent : drawColor;
            Paint glowBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowBg.setStyle(Paint.Style.FILL);
            glowBg.setColor(Color.argb(40, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)));
            canvas.drawRoundRect(r, 12, 12, glowBg);

            Paint selGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            selGlow.setStyle(Paint.Style.STROKE);
            selGlow.setStrokeWidth(4f);
            selGlow.setColor(glowColor);
            canvas.drawRoundRect(r, 12, 12, selGlow);
        } else {
            Paint normBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            normBorder.setStyle(Paint.Style.STROKE);
            normBorder.setStrokeWidth(branchAccent != 0 ? 2.5f : 1.5f);
            normBorder.setColor(branchAccent != 0
                    ? branchAccent
                    : Color.argb(35, 255, 255, 255));
            canvas.drawRoundRect(r, 12, 12, normBorder);
        }

        Bitmap iconBmp = getCategoryIconFromRes(n.categoryIconRes, n.categoryColor, n.name);
        float d = getResources().getDisplayMetrics().density;
        float iconSize = Math.min(26f * d, n.height * 0.40f);
        float iconX = n.x + (n.width - iconSize) / 2f;
        float iconY = n.y + 7f * d;

        if (iconBmp != null) {
            RectF dst = new RectF(iconX, iconY, iconX + iconSize, iconY + iconSize);
            canvas.drawBitmap(iconBmp, null, dst, null);
        }

        // TONE3000 compact T3K mark — top interior, small chip (spec §16 compact mark)
        if (t3kNodeIds.contains(n.pluginId)) {
            String t3k = "T3K";
            Paint t3kBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            t3kBg.setColor(Color.parseColor("#FF8A00"));
            t3kBg.setStyle(Paint.Style.FILL);
            Paint t3kText = new Paint(Paint.ANTI_ALIAS_FLAG);
            t3kText.setColor(Color.WHITE);
            t3kText.setTextSize(7.5f * d);
            t3kText.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
            t3kText.setTextAlign(Paint.Align.CENTER);
            float padH = 4f * d, padV = 2f * d;
            float tw = t3kText.measureText(t3k);
            float bw = tw + padH * 2, bh = 9f * d;
            float bx = n.x + 4f * d, by = n.y + 4f * d;
            android.graphics.RectF t3kR = new android.graphics.RectF(bx, by, bx + bw, by + bh);
            canvas.drawRoundRect(t3kR, 3f * d, 3f * d, t3kBg);
            Paint.FontMetrics fm = t3kText.getFontMetrics();
            float ty = by + bh/2f - (fm.ascent + fm.descent)/2f;
            canvas.drawText(t3k, bx + bw/2f, ty, t3kText);
        }

        // Label (compact, crisp, inside block bottom)
        String code = n.categoryCode != null ? n.categoryCode : getCategoryCode(n.name);
        paintText.setColor(n.bypassed ? Color.parseColor("#80FFFFFF") : Color.parseColor("#ECEFF1"));
         paintText.setTextSize(10f * d);
        canvas.drawText(code, n.x + n.width / 2f, n.y + n.height - 8f * d, paintText);

        // Port dots — density-scaled to match anchor nodes. Branch members
        // use the branch accent (R17 §2.3) so both sides of a split read A/B.
        float pr = 4.5f * getResources().getDisplayMetrics().density;
        paintInputDot.setColor(branchAccent != 0 ? branchAccent : drawColor);
        paintOutputDot.setColor(branchAccent != 0 ? branchAccent : drawColor);
        canvas.drawCircle(n.getInputX(), n.getInputY(), pr, paintInputDot);
        canvas.drawCircle(n.getOutputX(), n.getOutputY(), pr, paintOutputDot);

        // Routing blocks expose their extra port visually: SPLIT has two outputs,
        // MERGE has two inputs. This makes the fixed 1-in/2-out and 2-in/1-out
        // port topology readable at a glance without any dragging.
        if (n.nodeType == NodeType.SPLIT) {
            float sep = n.height * 0.26f;
            canvas.drawCircle(n.getOutputX(), n.getOutputY() - sep, pr * 0.85f, paintOutputDot);
            canvas.drawCircle(n.getOutputX(), n.getOutputY() + sep, pr * 0.85f, paintOutputDot);
        } else if (n.nodeType == NodeType.MERGE) {
            float sep = n.height * 0.26f;
            canvas.drawCircle(n.getInputX(), n.getInputY() - sep, pr * 0.85f, paintInputDot);
            canvas.drawCircle(n.getInputX(), n.getInputY() + sep, pr * 0.85f, paintInputDot);
        }

        // R17 §2.3: small A/B badge so a branch member is identifiable even
        // before following its wires.
        if (branchAccent != 0) {
            String tag = n.branch == 1 ? "A" : "B";
            Paint badgeBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeBg.setColor(branchAccent);
            Paint badgeText = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeText.setColor(Color.parseColor("#101318"));
            badgeText.setTextSize(7.5f * d);
            badgeText.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                    android.graphics.Typeface.BOLD));
            badgeText.setTextAlign(Paint.Align.CENTER);
            float bh = 10f * d;
            float bw = badgeText.measureText(tag) + 6f * d;
            float bx = n.x + n.width - bw - 4f * d;
            float by = n.y + 4f * d;
            canvas.drawRoundRect(new android.graphics.RectF(bx, by, bx + bw, by + bh),
                    3f * d, 3f * d, badgeBg);
            android.graphics.Paint.FontMetrics fm = badgeText.getFontMetrics();
            canvas.drawText(tag, bx + bw / 2f, by + bh / 2f - (fm.ascent + fm.descent) / 2f, badgeText);
        }

        if (n.selected) {
            float bx = n.x + n.width - 8 * d;
            float by = n.y - 4 * d;
            canvas.drawCircle(bx, by, 9 * d, paintDeleteBtn);
            canvas.drawLine(bx - 3.5f * d, by - 3.5f * d, bx + 3.5f * d, by + 3.5f * d, paintDeleteX);
            canvas.drawLine(bx + 3.5f * d, by - 3.5f * d, bx - 3.5f * d, by + 3.5f * d, paintDeleteX);
        }
    }

    private void drawBezierWire(Canvas canvas, float x1, float y1, float x2, float y2, int color) {        Path path = new Path();
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
