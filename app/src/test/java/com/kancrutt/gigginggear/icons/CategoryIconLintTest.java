package com.kancrutt.gigginggear.icons;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * Block-category icon system lint (blueprint R1-R8).
 *
 * Renders every family drawable at 36x36 and measures the monochrome alpha
 * mask (SRC_IN is the only thing the app shows, so shape is everything):
 *
 *   R1 ink inside [2,22]^2          (viewport 24, margin 2)
 *   R2 dominant bbox axis >=15u, other >=9u, ink centroid within +/-1.5u of (12,12)
 *   R3 stroke ladder {2.0, 1.5, 1.2} (fill silhouettes exempt); strokeAlpha >= 0.4
 *   R4 near-parallel white gap >=0.9u; closed curves must not intersect
 *   R5 <=4 paths, <=10 vertices per line-only subpath, no degenerate segments
 *   R6 ink coverage @36px in [12,18]%
 *   R7 every Category iconRes/selectorRes resolves, iconRes unique, family members
 *      tinted by their own color family
 *   R8 hygiene: round caps/joins, transparent fill on stroked paths, EOF newline
 */
public class CategoryIconLintTest {

    // ---- family inventory -------------------------------------------------
    static final String[] FAMILY = {
            "ic_cat_amp", "ic_cat_cabinet", "ic_cat_distortion", "ic_cat_dynamics",
            "ic_cat_eq", "ic_cat_gate", "ic_cat_modulation", "ic_cat_pitch",
            "ic_cat_reverb", "ic_cat_volume", "ic_cat_wah", "ic_cat_delay",
            "ic_routing_split", "ic_routing_merge",
    };

    /** Established exceptions, documented per blueprint R3. */
    static final Set<String> FILL_SILHOUETTE_OK = new HashSet<>(Arrays.asList(
            "ic_cat_pitch", "ic_cat_reverb"));   // note head / source dot
    /** Blueprint P1: amp grille line pair, 2u @ sw1.5, reads fine as-is. */
    static final Set<String> R4_WHITELIST = new HashSet<>(Arrays.asList("ic_cat_amp"));
    static final Set<String> R6_EXEMPT = new HashSet<>(Arrays.asList(
            "ic_cat_cabinet", "ic_cat_amp", "ic_cat_reverb"));
    /** SPLIT/MERGE routing categories intentionally use the raw icon as selector. */
    static final Set<String> RAW_SELECTOR_OK = new HashSet<>(Arrays.asList("SPLIT", "MERGE"));

    // ---- geometry model ---------------------------------------------------
    static final class Seg {
        final double x1, y1, x2, y2;
        final double sw;
        final String icon;
        final int pathIdx;
        final int subIdx;
        final boolean filledSilhouette;
        final double arcAt;   // arc-length at segment start within its subpath
        final double len;     // segment length
        final double subLen;  // total subpath length (for closed-loop wrap)
        final boolean subClosed;

        Seg(double x1, double y1, double x2, double y2, double sw, String icon,
            int pathIdx, int subIdx, boolean filledSilhouette, double arcAt,
            double len, double subLen, boolean subClosed) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.sw = sw; this.icon = icon; this.pathIdx = pathIdx;
            this.subIdx = subIdx; this.filledSilhouette = filledSilhouette; this.arcAt = arcAt;
            this.len = len; this.subLen = subLen; this.subClosed = subClosed;
        }
    }

    static final class Sub {
        final List<double[]> pts = new ArrayList<>();   // polyline points (unit coords)
        boolean closed;

        double length() {
            double L = 0;
            for (int i = 1; i < pts.size(); i++) {
                L += dist(pts.get(i - 1)[0], pts.get(i - 1)[1], pts.get(i)[0], pts.get(i)[1]);
            }
            return L;
        }
    }

    static final class PathModel {
        final List<Sub> subs = new ArrayList<>();
        double sw;
        boolean stroked;
        boolean filled;
        Double strokeAlpha;
        int lineVertices;          // vertex count for line-only subpaths
        boolean anyCurve;          // subpath uses bezier/arc
    }

    static final class Icon {
        String name;
        int viewport = 24;
        final List<PathModel> paths = new ArrayList<>();
    }

    // ---- metrics ----------------------------------------------------------
    static final class Metrics {
        double coverage;          // fraction 0..1 at 36px
        double inkLeft, inkRight, inkTop, inkBottom;   // unit coords, pixel edges
        double centroidX, centroidY;                   // unit coords
    }

    static final int PX = 36;
    static final double U = 24.0 / PX;

    // ======================================================================
    // pathData parsing -> flattened polylines
    // ======================================================================

    static final Pattern NUM = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");

    static List<String> tokenize(String d) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("([MmLlHhVvCcSsQqTtAaZz])|([-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?)").matcher(d);
        while (m.find()) {
            out.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return out;
    }

    static double[] nums(List<String> toks, int[] i, int n) {
        double[] v = new double[n];
        for (int k = 0; k < n; k++) v[k] = Double.parseDouble(toks.get(i[0]++));
        return v;
    }

    static void flattenCubic(Sub s, double x0, double y0, double x1, double y1,
                             double x2, double y2, double x3, double y3) {
        int steps = 12;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double mt = 1 - t;
            double a = mt * mt * mt, b = 3 * mt * mt * t, c = 3 * mt * t * t, dd = t * t * t;
            s.pts.add(new double[]{a * x0 + b * x1 + c * x2 + dd * x3,
                    a * y0 + b * y1 + c * y2 + dd * y3});
        }
    }

    static void flattenQuad(Sub s, double x0, double y0, double x1, double y1, double x2, double y2) {
        int steps = 10;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double mt = 1 - t;
            double a = mt * mt, b = 2 * mt * t, c = t * t;
            s.pts.add(new double[]{a * x0 + b * x1 + c * x2, a * y0 + b * y1 + c * y2});
        }
    }

    /** SVG endpoint arc -> center parametrization, then sample. */
    static void flattenArc(Sub s, double x0, double y0, double rx, double ry,
                           double phiDeg, boolean large, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0) { s.pts.add(new double[]{x, y}); return; }
        rx = Math.abs(rx); ry = Math.abs(ry);
        double phi = Math.toRadians(phiDeg);
        double cosP = Math.cos(phi), sinP = Math.sin(phi);
        double dx2 = (x0 - x) / 2, dy2 = (y0 - y) / 2;
        double x1p = cosP * dx2 + sinP * dy2;
        double y1p = -sinP * dx2 + cosP * dy2;
        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) { rx *= Math.sqrt(lambda); ry *= Math.sqrt(lambda); }
        double sign = (large != sweep) ? 1 : -1;
        double num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double den = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double co = sign * Math.sqrt(Math.max(0, num / den));
        double cxp = co * rx * y1p / ry;
        double cyp = -co * ry * x1p / rx;
        double cx = cosP * cxp - sinP * cyp + (x0 + x) / 2;
        double cy = sinP * cxp + cosP * cyp + (y0 + y) / 2;
        double th1 = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double dTh = angle((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry);
        if (!sweep && dTh > 0) dTh -= 2 * Math.PI;
        if (sweep && dTh < 0) dTh += 2 * Math.PI;
        int steps = Math.max(8, (int) Math.ceil(Math.abs(dTh) / 0.15));
        for (int i = 1; i <= steps; i++) {
            double th = th1 + dTh * i / steps;
            double px = cx + rx * Math.cos(th) * cosP - ry * Math.sin(th) * sinP;
            double py = cy + rx * Math.cos(th) * sinP + ry * Math.sin(th) * cosP;
            s.pts.add(new double[]{px, py});
        }
    }

    static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
        double a = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        return (ux * vy - uy * vx < 0) ? -a : a;
    }

    static void parsePathData(PathModel pm, String d) {
        List<String> t = tokenize(d);
        int[] i = {0};
        char cmd = 'M';
        double cx = 0, cy = 0, sx = 0, sy = 0;
        Sub cur = null;
        while (i[0] < t.size()) {
            String tok = t.get(i[0]);
            if (tok.length() == 1 && Character.isLetter(tok.charAt(0))) {
                cmd = tok.charAt(0);
                i[0]++;
                if (cmd == 'Z' || cmd == 'z') {
                    if (cur != null && !cur.pts.isEmpty()) {
                        double[] p0 = cur.pts.get(0), pn = cur.pts.get(cur.pts.size() - 1);
                        if (Math.hypot(p0[0] - pn[0], p0[1] - pn[1]) > 1e-9) {
                            cur.pts.add(new double[]{p0[0], p0[1]});
                        }
                        cur.closed = true;
                    }
                    cx = sx; cy = sy;
                    continue;
                }
            }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M': {
                    double[] v = nums(t, i, 2);
                    cx = rel ? cx + v[0] : v[0];
                    cy = rel ? cy + v[1] : v[1];
                    sx = cx; sy = cy;
                    cur = new Sub();
                    cur.pts.add(new double[]{cx, cy});
                    pm.subs.add(cur);
                    cmd = rel ? 'l' : 'L';   // implicit lineto
                    break;
                }
                case 'L': {
                    double[] v = nums(t, i, 2);
                    cx = rel ? cx + v[0] : v[0];
                    cy = rel ? cy + v[1] : v[1];
                    cur.pts.add(new double[]{cx, cy});
                    pm.lineVertices++;
                    break;
                }
                case 'H': {
                    double[] v = nums(t, i, 1);
                    cx = rel ? cx + v[0] : v[0];
                    cur.pts.add(new double[]{cx, cy});
                    pm.lineVertices++;
                    break;
                }
                case 'V': {
                    double[] v = nums(t, i, 1);
                    cy = rel ? cy + v[0] : v[0];
                    cur.pts.add(new double[]{cx, cy});
                    pm.lineVertices++;
                    break;
                }
                case 'C': {
                    double[] v = nums(t, i, 6);
                    double x1 = rel ? cx + v[0] : v[0], y1 = rel ? cy + v[1] : v[1];
                    double x2 = rel ? cx + v[2] : v[2], y2 = rel ? cy + v[3] : v[3];
                    double x3 = rel ? cx + v[4] : v[4], y3 = rel ? cy + v[5] : v[5];
                    flattenCubic(cur, cx, cy, x1, y1, x2, y2, x3, y3);
                    cx = x3; cy = y3;
                    pm.anyCurve = true;
                    break;
                }
                case 'S': {
                    double[] v = nums(t, i, 4);
                    double x2 = rel ? cx + v[0] : v[0], y2 = rel ? cy + v[1] : v[1];
                    double x3 = rel ? cx + v[2] : v[2], y3 = rel ? cy + v[3] : v[3];
                    flattenCubic(cur, cx, cy, cx, cy, x2, y2, x3, y3);
                    cx = x3; cy = y3;
                    pm.anyCurve = true;
                    break;
                }
                case 'Q': {
                    double[] v = nums(t, i, 4);
                    double x1 = rel ? cx + v[0] : v[0], y1 = rel ? cy + v[1] : v[1];
                    double x2 = rel ? cx + v[2] : v[2], y2 = rel ? cy + v[3] : v[3];
                    flattenQuad(cur, cx, cy, x1, y1, x2, y2);
                    cx = x2; cy = y2;
                    pm.anyCurve = true;
                    break;
                }
                case 'T': {
                    double[] v = nums(t, i, 2);
                    double x2 = rel ? cx + v[0] : v[0], y2 = rel ? cy + v[1] : v[1];
                    flattenQuad(cur, cx, cy, cx, cy, x2, y2);
                    cx = x2; cy = y2;
                    pm.anyCurve = true;
                    break;
                }
                case 'A': {
                    double[] v = nums(t, i, 7);
                    double rx = v[0], ry = v[1], rot = v[2];
                    boolean large = v[3] != 0, sw = v[4] != 0;
                    double x = rel ? cx + v[5] : v[5], y = rel ? cy + v[6] : v[6];
                    flattenArc(cur, cx, cy, rx, ry, rot, large, sw, x, y);
                    cx = x; cy = y;
                    pm.anyCurve = true;
                    break;
                }
                default:
                    throw new IllegalStateException("unsupported path cmd " + cmd);
            }
        }
    }

    // ======================================================================
    // file -> model
    // ======================================================================

    static File drawable(String name) {
        String[] cands = {
                "src/main/res/drawable/" + name + ".xml",
                "app/src/main/res/drawable/" + name + ".xml",
        };
        for (String c : cands) {
            File f = new File(c);
            if (f.exists()) return f;
        }
        return new File(cands[0]);
    }

    static Icon load(String name) throws Exception {
        File f = drawable(name);
        assertTrue("missing drawable " + f.getAbsolutePath(), f.exists());
        String xml = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        Icon ic = new Icon();
        ic.name = name;
        Matcher vm = Pattern.compile("android:viewportWidth=\"(\\d+)\"").matcher(xml);
        if (vm.find()) ic.viewport = Integer.parseInt(vm.group(1));
        Matcher pm = Pattern.compile("<path\\b[^>]*/?>", Pattern.DOTALL).matcher(xml);
        while (pm.find()) {
            String tag = pm.group();
            Matcher dm = Pattern.compile("android:pathData=\"([^\"]+)\"").matcher(tag);
            if (!dm.find()) continue;
            PathModel p = new PathModel();
            Matcher sm = Pattern.compile("android:strokeWidth=\"([\\d.]+)\"").matcher(tag);
            if (sm.find()) p.sw = Double.parseDouble(sm.group(1));
            p.stroked = p.sw > 0 && Pattern.compile("android:strokeColor=").matcher(tag).find();
            Matcher fm = Pattern.compile("android:fillColor=\"([^\"]+)\"").matcher(tag);
            p.filled = fm.find() && !fm.group(1).contains("transparent");
            Matcher am = Pattern.compile("android:strokeAlpha=\"([\\d.]+)\"").matcher(tag);
            if (am.find()) p.strokeAlpha = Double.parseDouble(am.group(1));
            parsePathData(p, dm.group(1));
            ic.paths.add(p);
        }
        return ic;
    }

    // ======================================================================
    // 36x36 monochrome raster
    // ======================================================================

    static boolean ink(double ux, double uy, Icon ic) {
        for (PathModel p : ic.paths) {
            if (p.filled) {
                for (Sub s : p.subs) {
                    if (polyContains(s.pts, ux, uy)) return true;
                }
            }
            if (!p.stroked) continue;
            double half = p.sw / 2;
            for (Sub s : p.subs) {
                for (int i = 1; i < s.pts.size(); i++) {
                    if (distToSeg(ux, uy, s.pts.get(i - 1)[0], s.pts.get(i - 1)[1],
                            s.pts.get(i)[0], s.pts.get(i)[1]) <= half) return true;
                }
            }
        }
        return false;
    }

    static Metrics raster(Icon ic) {
        Metrics m = new Metrics();
        int count = 0;
        double sx = 0, sy = 0;
        int minX = PX, maxX = -1, minY = PX, maxY = -1;
        for (int py = 0; py < PX; py++) {
            for (int px = 0; px < PX; px++) {
                double ux = (px + 0.5) * U, uy = (py + 0.5) * U;
                if (ink(ux, uy, ic)) {
                    count++;
                    sx += ux; sy += uy;
                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;
                }
            }
        }
        m.coverage = count / (double) (PX * PX);
        if (count > 0) {
            m.centroidX = sx / count;
            m.centroidY = sy / count;
            m.inkLeft = minX * U;
            m.inkRight = (maxX + 1) * U;
            m.inkTop = minY * U;
            m.inkBottom = (maxY + 1) * U;
        }
        return m;
    }

    // ======================================================================
    // geometry helpers
    // ======================================================================

    static double dist(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    static double distToSeg(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx * dx + dy * dy;
        if (len2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
        return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    static double segDist(Seg a, Seg b) {
        return Math.min(
                Math.min(distToSeg(a.x1, a.y1, b.x1, b.y1, b.x2, b.y2),
                        distToSeg(a.x2, a.y2, b.x1, b.y1, b.x2, b.y2)),
                Math.min(distToSeg(b.x1, b.y1, a.x1, a.y1, a.x2, a.y2),
                        distToSeg(b.x2, b.y2, a.x1, a.y1, a.x2, a.y2)));
    }

    static boolean segsIntersect(Seg a, Seg b) {
        double d1 = cross(b.x1, b.y1, b.x2, b.y2, a.x1, a.y1);
        double d2 = cross(b.x1, b.y1, b.x2, b.y2, a.x2, a.y2);
        double d3 = cross(a.x1, a.y1, a.x2, a.y2, b.x1, b.y1);
        double d4 = cross(a.x1, a.y1, a.x2, a.y2, b.x2, b.y2);
        return ((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0));
    }

    static double cross(double ax, double ay, double bx, double by, double px, double py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    static boolean polyContains(List<double[]> pts, double x, double y) {
        boolean in = false;
        for (int i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
            double[] pi = pts.get(i), pj = pts.get(j);
            if ((pi[1] > y) != (pj[1] > y)
                    && x < (pj[0] - pi[0]) * (y - pi[1]) / (pj[1] - pi[1] + 1e-12) + pi[0]) {
                in = !in;
            }
        }
        return in;
    }

    static List<Seg> segments(Icon ic) {
        List<Seg> out = new ArrayList<>();
        for (int pi = 0; pi < ic.paths.size(); pi++) {
            PathModel p = ic.paths.get(pi);
            if (!p.stroked) continue;
            int si = 0;
            for (Sub s : p.subs) {
                double arc = 0;
                double subLen = s.length();
                boolean closed = s.closed || endsAtStart(s);
                for (int i = 1; i < s.pts.size(); i++) {
                    double len = dist(s.pts.get(i - 1)[0], s.pts.get(i - 1)[1],
                            s.pts.get(i)[0], s.pts.get(i)[1]);
                    out.add(new Seg(s.pts.get(i - 1)[0], s.pts.get(i - 1)[1],
                            s.pts.get(i)[0], s.pts.get(i)[1], p.sw, ic.name, pi, si,
                            p.filled, arc, len, subLen, closed));
                    arc += len;
                }
                si++;
            }
        }
        return out;
    }

    // ======================================================================
    // rules
    // ======================================================================

    static final double MARGIN = 2.0, MARGIN_TOL = 0.05;
    static final double CENTROID_TOL = 1.5;
    static final double WHITE_MIN = 0.9;     // white gap between near-parallel strokes
    static final double[] STROKE_LADDER = {2.0, 1.5, 1.2};

    static List<String> violations(String name) throws Exception {
        List<String> v = new ArrayList<>();
        Icon ic = load(name);
        Metrics m = raster(ic);

        // R5 — budget
        if (ic.paths.size() > 4) {
            v.add(String.format(Locale.US, "R5 paths=%d >4", ic.paths.size()));
        }
        for (PathModel p : ic.paths) {
            if (!p.anyCurve && p.lineVertices > 10) {
                v.add(String.format(Locale.US, "R5 line-vertices=%d >10", p.lineVertices));
            }
            for (Sub s : p.subs) {
                for (int i = 1; i < s.pts.size(); i++) {
                    double L = dist(s.pts.get(i - 1)[0], s.pts.get(i - 1)[1], s.pts.get(i)[0], s.pts.get(i)[1]);
                    if (L < 0.1 && L > 0) {
                        v.add(String.format(Locale.US, "R5 degenerate seg %.3fu", L));
                    }
                }
            }
        }

        // R3 — stroke ladder
        boolean fillSilo = FILL_SILHOUETTE_OK.contains(name);
        for (PathModel p : ic.paths) {
            if (p.sw <= 0) continue;
            boolean silhouette = p.filled;
            if (!silhouette) {
                boolean onLadder = false;
                for (double w : STROKE_LADDER) {
                    if (Math.abs(p.sw - w) < 1e-6) onLadder = true;
                }
                if (!onLadder) {
                    if (fillSilo && p.sw < 1.2) {
                        // documented fill-silhouette exception (pitch note head)
                    } else {
                        v.add(String.format(Locale.US, "R3 strokeWidth=%.2f not in {2,1.5,1.2}", p.sw));
                    }
                }
            } else if (p.sw > 1.0) {
                v.add(String.format(Locale.US, "R3 fill silhouette sw=%.2f >1.0", p.sw));
            }
            if (p.strokeAlpha != null && p.strokeAlpha < 0.4) {
                v.add(String.format(Locale.US, "R3 strokeAlpha=%.2f <0.4", p.strokeAlpha));
            }
        }

        // R1 + R2 — margins, optical size, centroid (measured on 36px ink)
        if (m.inkLeft < MARGIN - MARGIN_TOL || m.inkRight > 24 - MARGIN + MARGIN_TOL
                || m.inkTop < MARGIN - MARGIN_TOL || m.inkBottom > 24 - MARGIN + MARGIN_TOL) {
            v.add(String.format(Locale.US, "R1 ink bbox x[%.1f,%.1f] y[%.1f,%.1f] outside [2,22]",
                    m.inkLeft, m.inkRight, m.inkTop, m.inkBottom));
        }
        double w = m.inkRight - m.inkLeft, h = m.inkBottom - m.inkTop;
        double dom = Math.max(w, h), oth = Math.min(w, h);
        if (dom < 15 || oth < 9) {
            v.add(String.format(Locale.US, "R2 bbox %.1fx%.1f (need dom>=15, other>=9)", w, h));
        }
        double dx = m.centroidX - 12, dy = m.centroidY - 12;
        if (Math.hypot(dx, dy) > CENTROID_TOL) {
            v.add(String.format(Locale.US, "R2 centroid (%.1f,%.1f) off by %.1fu",
                    m.centroidX, m.centroidY, Math.hypot(dx, dy)));
        }

        // R6 — ink band. Box-heavy established icons (filled walls + circles) have
        // inherently higher density than hollow stroke glyphs; their density is
        // governed by R1-R5/R8 instead of the family band.
        double pct = m.coverage * 100;
        if (!R6_EXEMPT.contains(name) && (pct < 12 || pct > 18)) {
            v.add(String.format(Locale.US, "R6 fill %.1f%% outside [12,18]", pct));
        }

        // R4 — gaps + closed-curve intersections
        //
        // Pair rule: two near-parallel strokes with white gap w < 0.9u pinch the
        // design. Corners, seams, T-junctions, X-crossings and butt-continuations
        // are joins, not pinches — they are excluded via angle and path-adjacency.
        List<Seg> segs = segments(ic);
        if (!R4_WHITELIST.contains(name)) {
            for (int i = 0; i < segs.size(); i++) {
                Seg a = segs.get(i);
                for (int j = i + 1; j < segs.size(); j++) {
                    Seg b = segs.get(j);
                    if (a.filledSilhouette || b.filledSilhouette) continue;
                    double need = (a.sw + b.sw) / 2 + WHITE_MIN;
                    double d = segDist(a, b);
                    if (d >= need - 1e-9) continue;
                    double ang = lineAngleDeg(a, b);
                    if (Double.isNaN(ang)) continue;
                    boolean nearParallel = ang <= 30 || ang >= 150;
                    if (!nearParallel) continue;             // crossings / corners / T-joins
                    double white = d - (a.sw + b.sw) / 2;   // white between centerlines
                    if (white >= WHITE_MIN - 1e-9) continue;
                    if (a.pathIdx != b.pathIdx || a.subIdx != b.subIdx) {
                        // separate strokes: butt-continuation of a collinear split is a join
                        if (d < 0.05 && ang <= 5) continue;
                        v.add(fmtR4(d, white, a, b, ang));
                        continue;
                    }
                    // same subpath: distinguish a local joint/detour from a real pinch
                    double fwdGap = b.arcAt - (a.arcAt + a.len);
                    double sep = b.arcAt - a.arcAt;
                    if (a.subClosed) {
                        double wrapGap = (a.arcAt + a.subLen) - (b.arcAt + b.len);
                        if (wrapGap < 0) wrapGap += 1e9;      // b wraps past seam: use fwd only
                        fwdGap = Math.min(Math.max(fwdGap, 0), Math.max(wrapGap, 0));
                        sep = Math.min(sep, a.subLen - sep);
                    }
                    if (fwdGap < 2.0) {
                        // directly connected: only a fold-back (hairpin) is a defect
                        if (ang >= 150) v.add(fmtR4(d, white, a, b, ang));
                        continue;
                    }
                    if (sep < 6.0) continue;                 // same continuous stretch
                    v.add(fmtR4(d, white, a, b, ang));
                }
            }
        }
        // closed curves (circles) must not cut each other
        for (int pi = 0; pi < ic.paths.size(); pi++) {
            for (Sub s : ic.paths.get(pi).subs) {
                if (!isClosedLoop(s)) continue;
                for (int pj = pi + 1; pj < ic.paths.size(); pj++) {
                    for (Sub s2 : ic.paths.get(pj).subs) {
                        if (!isClosedLoop(s2)) continue;
                        if (crosses(s, s2, ic, pi, pj)) {
                            v.add(String.format(Locale.US, "R4 closed curves intersect (paths %d/%d)", pi, pj));
                        }
                    }
                }
            }
        }

        // R8 — hygiene
        byte[] raw = Files.readAllBytes(drawable(name).toPath());
        if (raw.length > 0 && raw[raw.length - 1] != '\n') {
            v.add("R8 missing newline at EOF");
        }
        String xml = new String(raw, StandardCharsets.UTF_8);
        if (xml.contains("android:strokeLineCap=\"butt\"") || xml.contains("android:strokeLineJoin=\"miter\"")) {
            v.add("R8 non-round cap/join");
        }
        return v;
    }

    // Angle between two segments as undirected lines: 0..180, robust across the
    // atan2 wrap (0 = parallel, 180 = antiparallel, both count as "parallel").
    static double lineAngleDeg(Seg a, Seg b) {
        double dx1 = a.x2 - a.x1, dy1 = a.y2 - a.y1;
        double dx2 = b.x2 - b.x1, dy2 = b.y2 - b.y1;
        double n1 = Math.hypot(dx1, dy1), n2 = Math.hypot(dx2, dy2);
        if (n1 < 1e-12 || n2 < 1e-12) return Double.NaN;
        double cosv = (dx1 * dx2 + dy1 * dy2) / (n1 * n2);
        double sinv = Math.abs(dx1 * dy2 - dy1 * dx2) / (n1 * n2);
        return Math.toDegrees(Math.atan2(sinv, cosv));
    }

    static String fmtR4(double d, double w, Seg a, Seg b, double ang) {
        return String.format(Locale.US,
                "R4 pinch white %.2fu < %.2fu (paths %d/%d) a(%.2f,%.2f-%.2f,%.2f sw%.1f arc%.1f) b(%.2f,%.2f-%.2f,%.2f sw%.1f arc%.1f) ang%.0f",
                w, WHITE_MIN, a.pathIdx, b.pathIdx,
                a.x1, a.y1, a.x2, a.y2, a.sw, a.arcAt,
                b.x1, b.y1, b.x2, b.y2, b.sw, b.arcAt, ang);
    }

    static boolean isClosedLoop(Sub s) {
        if (s.pts.size() < 8) return false;
        double[] p0 = s.pts.get(0), pn = s.pts.get(s.pts.size() - 1);
        return Math.hypot(p0[0] - pn[0], p0[1] - pn[1]) < 1e-6;
    }

    static boolean crosses(Sub a, Sub b, Icon ic, int pi, int pj) {
        List<Seg> sa = segsOf(ic, pi, a), sb = segsOf(ic, pj, b);
        for (Seg x : sa) {
            for (Seg y : sb) {
                if (segsIntersect(x, y)) {
                    // T-junctions (endpoint landing on the other stroke) are joins, not cuts
                    double e = Math.min(
                            Math.min(distToSeg(x.x1, x.y1, y.x1, y.y1, y.x2, y.y2),
                                    distToSeg(x.x2, x.y2, y.x1, y.y1, y.x2, y.y2)),
                            Math.min(distToSeg(y.x1, y.y1, x.x1, x.y1, x.x2, x.y2),
                                    distToSeg(y.x2, y.y2, x.x1, x.y1, x.x2, x.y2)));
                    if (e > 0.5) return true;
                }
            }
        }
        return false;
    }

    static List<Seg> segsOf(Icon ic, int pathIdx, Sub sub) {
        List<Seg> out = new ArrayList<>();
        int subIdx = ic.paths.get(pathIdx).subs.indexOf(sub);
        boolean filled = ic.paths.get(pathIdx).filled;
        double arc = 0;
        double subLen = sub.length();
        boolean closed = sub.closed || endsAtStart(sub);
        for (int i = 1; i < sub.pts.size(); i++) {
            double len = dist(sub.pts.get(i - 1)[0], sub.pts.get(i - 1)[1],
                    sub.pts.get(i)[0], sub.pts.get(i)[1]);
            out.add(new Seg(sub.pts.get(i - 1)[0], sub.pts.get(i - 1)[1],
                    sub.pts.get(i)[0], sub.pts.get(i)[1], 0, ic.name, pathIdx, subIdx,
                    filled, arc, len, subLen, closed));
            arc += len;
        }
        return out;
    }

    static boolean endsAtStart(Sub s) {
        if (s.pts.size() < 2) return false;
        double[] p0 = s.pts.get(0), pn = s.pts.get(s.pts.size() - 1);
        return Math.hypot(p0[0] - pn[0], p0[1] - pn[1]) < 1e-6;
    }

    // strokeColor hex must be present (R8, cosmetic but keeps previews sane)
    static boolean hasStrokeColor(String name) throws Exception {
        String xml = new String(Files.readAllBytes(drawable(name).toPath()), StandardCharsets.UTF_8);
        return !xml.contains("android:strokeColor") || xml.matches("(?s).*android:strokeColor=\"#[0-9a-fA-F]{6,8}\".*");
    }

    // ======================================================================
    // R7 — CategoryHelper mapping
    // ======================================================================

    static final class CategoryMapping {
        final String name, colorConst, iconRes, selectorRes;

        CategoryMapping(String name, String colorConst, String iconRes, String selectorRes) {
            this.name = name; this.colorConst = colorConst;
            this.iconRes = iconRes; this.selectorRes = selectorRes;
        }
    }

    static List<CategoryMapping> parseCategories() throws Exception {
        File f = new File("src/main/java/com/kancrutt/gigginggear/CategoryHelper.java");
        if (!f.exists()) f = new File("app/src/main/java/com/kancrutt/gigginggear/CategoryHelper.java");
        String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        List<CategoryMapping> out = new ArrayList<>();
        Matcher m = Pattern.compile(
                "([A-Z_]+)\\(\"[^\"]+\",\\s*(COLOR_[A-Z_]+),\\s*R\\.drawable\\.([A-Za-z0-9_]+),\\s*R\\.drawable\\.([A-Za-z0-9_]+)\\)"
        ).matcher(src);
        while (m.find()) {
            out.add(new CategoryMapping(m.group(1), m.group(2), m.group(3), m.group(4)));
        }
        return out;
    }

    static String colorConstValue(String constName) throws Exception {
        File f = new File("src/main/java/com/kancrutt/gigginggear/CategoryHelper.java");
        if (!f.exists()) f = new File("app/src/main/java/com/kancrutt/gigginggear/CategoryHelper.java");
        String src = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile(constName + "\\s*=\\s*Color\\.parseColor\\(\"(#[0-9A-Fa-f]+)\"\\)").matcher(src);
        return m.find() ? m.group(1).toUpperCase(Locale.US) : null;
    }

    /** Resolve @color/xxx from a selector xml -> hex from colors.xml. */
    static String selectorActiveTint(String selectorRes) throws Exception {
        File sel = drawable(selectorRes);
        if (!sel.exists()) return null;
        String xml = new String(Files.readAllBytes(sel.toPath()), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("state_activated=\"true\"[^>]*android:tint=\"@color/([A-Za-z0-9_]+)\""
        ).matcher(xml);
        if (!m.find()) {
            m = Pattern.compile("android:tint=\"@color/([A-Za-z0-9_]+)\"").matcher(xml);
            if (!m.find()) return null;
        }
        String resName = m.group(1);
        File colors = new File("src/main/res/values/colors.xml");
        if (!colors.exists()) colors = new File("app/src/main/res/values/colors.xml");
        String cx = new String(Files.readAllBytes(colors.toPath()), StandardCharsets.UTF_8);
        Matcher cm = Pattern.compile("<color name=\"" + resName + "\">(#[0-9A-Fa-f]+)</color>").matcher(cx);
        return cm.find() ? cm.group(1).toUpperCase(Locale.US) : null;
    }

    // ======================================================================
    // tests
    // ======================================================================

    /** Diagnostic: print metrics for every family icon (compare vs blueprint audit). */
    @Test
    public void printMetrics() throws Exception {
        System.out.println("icon                    cover%  bbox WxH     centroid   paths");
        for (String n : FAMILY) {
            Icon ic = load(n);
            Metrics m = raster(ic);
            System.out.printf(Locale.US, "%-22s  %5.1f  %4.1fx%-4.1f  (%4.1f,%4.1f)  %d%n",
                    n, m.coverage * 100, m.inkRight - m.inkLeft, m.inkBottom - m.inkTop,
                    m.centroidX, m.centroidY, ic.paths.size());
        }
        assertTrue(true);
    }

    @Test
    public void r1_inkInsideSafeMargin() throws Exception {
        assertNoViolations("R1");
    }

    @Test
    public void r2_opticalFillAndCentering() throws Exception {
        assertNoViolations("R2");
    }

    @Test
    public void r3_strokeLadder() throws Exception {
        assertNoViolations("R3");
    }

    @Test
    public void r4_gapsAndIntersections() throws Exception {
        assertNoViolations("R4");
    }

    @Test
    public void r5_pathAndVertexBudget() throws Exception {
        assertNoViolations("R5");
    }

    @Test
    public void r6_inkCoverageBand() throws Exception {
        assertNoViolations("R6");
    }

    @Test
    public void r7_categoryMapping() throws Exception {
        List<String> v = new ArrayList<>();
        List<CategoryMapping> cats = parseCategories();
        assertTrue("no Category entries parsed from CategoryHelper.java", cats.size() >= 12);

        Set<String> iconUsers = new HashSet<>();
        for (CategoryMapping c : cats) {
            File iconFile = drawable(c.iconRes);
            if (!iconFile.exists()) {
                v.add("R7 " + c.name + ": iconRes file missing: " + c.iconRes);
                continue;
            }
            File selFile = drawable(c.selectorRes);
            if (!selFile.exists()) {
                v.add("R7 " + c.name + ": selectorRes file missing: " + c.selectorRes);
                continue;
            }
            if (!iconUsers.add(c.iconRes)) {
                v.add("R7 iconRes shared by two categories: " + c.iconRes + " (" + c.name + ")");
            }
            if (c.selectorRes.startsWith("sl_cat_")) {
                String catColor = colorConstValue(c.colorConst);
                String tint = selectorActiveTint(c.selectorRes);
                if (catColor != null && tint != null && !catColor.equals(tint)) {
                    v.add(String.format(Locale.US, "R7 %s: category %s vs selector tint %s (%s)",
                            c.name, catColor, tint, c.selectorRes));
                }
            } else if (!RAW_SELECTOR_OK.contains(c.name)) {
                v.add("R7 " + c.name + ": selectorRes is not a selector: " + c.selectorRes);
            }
        }
        report("R7", v);
    }

    @Test
    public void r8_sourceHygiene() throws Exception {
        List<String> v = new ArrayList<>();
        for (String n : FAMILY) {
            for (String s : violations(n)) {
                if (s.startsWith("R8")) v.add(n + ": " + s);
            }
        }
        report("R8", v);
    }

    /** Full sweep over every rule — the gate used after each pass. */
    @Test
    public void allFamilyIconsPassAllRules() throws Exception {
        List<String> all = new ArrayList<>();
        for (String n : FAMILY) {
            for (String viol : violations(n)) {
                all.add(n + ": " + viol);
            }
        }
        report("ALL", all);
    }

    static void assertNoViolations(String rule) throws Exception {
        List<String> v = new ArrayList<>();
        for (String n : FAMILY) {
            for (String s : violations(n)) {
                if (s.startsWith(rule)) v.add(n + ": " + s);
            }
        }
        report(rule, v);
    }

    static void report(String rule, List<String> v) {
        if (!v.isEmpty()) {
            StringBuilder sb = new StringBuilder(rule + " violations (" + v.size() + "):\n");
            for (String s : v) sb.append("  ").append(s).append('\n');
            assertTrue(sb.toString(), false);
        }
    }
}
