package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.system.P;
import common.util.anim.EPart;

public class RawPointGetter {
    P rightUp, rightDown, leftUp, leftDown, center;
    double rawSizX = 1.0, rawSizY = 1.0, rawAngle;

    double flipX = 1.0, flipY = 1.0;

    final double w, h;

    public RawPointGetter(int w, int h) {
        this.w = w;
        this.h = h;

        rightDown = new P(w, h);
        rightUp = new P(w, 0.0);
        leftUp = new P(0.0, 0.0);
        leftDown = new P(0.0, h);
        center = new P(0.0, 0.0);
    }

    public void translate(double x, double y) {
        rightUp = translatePoint(flipX * flipY * rawAngle, x, rightUp);
        rightUp = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, y, rightUp);

        rightDown = translatePoint(flipX * flipY * rawAngle, x, rightDown);
        rightDown = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, y, rightDown);

        leftUp = translatePoint(flipX * flipY * rawAngle, x, leftUp);
        leftUp = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, y, leftUp);

        leftDown = translatePoint(flipX * flipY * rawAngle, x, leftDown);
        leftDown = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, y, leftDown);

        center = translatePoint(flipX * flipY * rawAngle, x, center);
        center = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, y, center);
    }

    public void translatePivot(double x, double y) {
        rightUp = translatePoint(flipX * flipY * rawAngle, -x, rightUp);
        rightUp = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, -y, rightUp);

        rightDown = translatePoint(flipX * flipY * rawAngle, -x, rightDown);
        rightDown = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, -y, rightDown);

        leftUp = translatePoint(flipX * flipY * rawAngle, -x, leftUp);
        leftUp = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, -y, leftUp);

        leftDown = translatePoint(flipX * flipY * rawAngle, -x, leftDown);
        leftDown = translatePoint(flipX * flipY * rawAngle + Math.PI / 2, -y, leftDown);
    }

    public void size(double sizX, double sizY) {
        if(sizX < 0)
            flipX *= -1;

        if(sizY < 0)
            flipY *= -1;

        rightUp = scalePoint(sizX, sizY, rightUp);
        rightDown = scalePoint(sizX, sizY, rightDown);
        leftUp = scalePoint(sizX, sizY, leftUp);
        leftDown = scalePoint(sizX, sizY, leftDown);
    }

    public void finalSize(double sizX, double sizY) {
        rawSizX *= sizX;
        rawSizY *= sizY;

        rightUp = scalePoint(rawSizX, rawSizY, rightUp);
        rightDown = scalePoint(rawSizX, rawSizY, rightDown);
        leftUp = scalePoint(rawSizX, rawSizY, leftUp);
        leftDown = scalePoint(rawSizX, rawSizY, leftDown);
    }

    public void wholeScale(double size) {
        rightUp.times(size);
        rightDown.times(size);
        leftUp.times(size);
        leftDown.times(size);
        center.times(size);
    }

    public void rotate(double angle) {
        rawAngle += angle;

        rightUp = rotatePoint(angle, rightUp);
        rightDown = rotatePoint(angle, rightDown);
        leftUp = rotatePoint(angle, leftUp);
        leftDown = rotatePoint(angle, leftDown);
    }

    public P rotatePoint(double angle, P point) {
        P res = new P(0.0, 0.0);

        res.x = center.x + (Math.cos(angle) * (point.x - center.x)) - (Math.sin(angle) * (point.y - center.y));
        res.y = center.y + (Math.cos(angle) * (point.y - center.y)) + (Math.sin(angle) * (point.x - center.x));

        return res;
    }

    public P scalePoint(double sizX, double sizY, P point) {
        double oldAngle = rawAngle;

        P res = rotatePoint(-oldAngle, point);

        res.x = center.x + (sizX * (res.x - center.x));
        res.y = center.y + (sizY * (res.y - center.y));

        res = rotatePoint(oldAngle, res);

        return res;
    }

    public P translatePoint(double angle, double d, P point) {
        P p = new P(0.0, 0.0);

        p.x = point.x + (flipX * d * Math.cos(angle) * rawSizX);
        p.y = point.y + (flipY * d * Math.sin(angle) * rawSizY);

        return p;
    }

    public P getSize(EPart p) {
        double mi = 1.0 / p.getModel().ints[0];

        if(p.getFa() == null) {
            return new P(p.getVal(9), p.getVal(10)).times(p.getVal(8) * mi * mi);
        }

        return getSize(p.getFa()).times(new P(p.getVal(9), p.getVal(10))).times(p.getVal(8) * mi * mi);
    }

    public void apply(EPart p, double size, boolean parent) {
        if(p.opa() <  CommonStatic.getConfig().deadOpa * 0.01 + 1e-5) {
            rightUp = new P(0, 0);
            rightDown = new P(0, 0);
            leftUp = new P(0, 0);
            leftDown = new P(0, 0);
            center = new P(0, 0);

            return;
        }

        P siz = new P(1.0, 1.0);

        if(p.getFa() != null) {
            apply(p.getFa(), 1.0, true);
            siz = getSize(p.getFa());
        }

        P tPos = new P(p.getVal(4), p.getVal(5)).times(siz);

        if(p.getParts()[0] != p) {
            translate(tPos.x, tPos.y);
            size(p.getVal(13), p.getVal(14));
        } else {
            if(p.getModel().confs.length > 0) {
                int[] data = p.getModel().confs[0];
                P p0 = getSize(p);
                P shi = new P(data[2], data[3]).times(p0);
                P p3 = shi.times(siz);
                translate(-p3.x, -p3.y);
            }

            P p0 = getSize(p);
            P p1 = new P(p.getVal(6), p.getVal(7)).times(p0).times(siz);

            translate(p1.x, p1.y);
        }

        if(p.getVal(11) != 0) {
            rotate(flipX * flipY * Math.PI * 2 * p.getVal(11) / p.getModel().ints[1]);
        }

        if(!parent) {
            P piv = new P(p.getVal(6), p.getVal(7));
            P scale = getSize(p);

            translatePivot(piv.x, piv.y);
            finalSize(scale.x, scale.y);
            wholeScale(size);
        }
    }

    public int[][] getRect() {
        int[][] rect = new int[4][2];

        rect[0][0] = (int) Math.round(leftUp.x);
        rect[0][1] = (int) Math.round(leftUp.y);
        rect[1][0] = (int) Math.round(rightUp.x);
        rect[1][1] = (int) Math.round(rightUp.y);
        rect[2][0] = (int) Math.round(rightDown.x);
        rect[2][1] = (int) Math.round(rightDown.y);
        rect[3][0] = (int) Math.round(leftDown.x);
        rect[3][1] = (int) Math.round(leftDown.y);

        return rect;
    }
}
