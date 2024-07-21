package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.system.P;
import common.util.anim.EPart;
import common.util.anim.MaModel;

public class RawPointGetter {
    P rightUp, rightDown, leftUp, leftDown, center;
    float rawSizX = 1f, rawSizY = 1f, rawAngle;

    float flipX = 1f, flipY = 1f;

    public RawPointGetter(int w, int h) {
        rightDown = new P(w, h);
        rightUp = new P(w, 0f);
        leftUp = new P(0f, 0f);
        leftDown = new P(0f, h);
        center = new P(0f, 0f);
    }

    public void translate(float x, float y) {
        rightUp = translatePoint(flipX * flipY * rawAngle, x, rightUp);
        rightUp = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), y, rightUp);

        rightDown = translatePoint(flipX * flipY * rawAngle, x, rightDown);
        rightDown = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), y, rightDown);

        leftUp = translatePoint(flipX * flipY * rawAngle, x, leftUp);
        leftUp = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), y, leftUp);

        leftDown = translatePoint(flipX * flipY * rawAngle, x, leftDown);
        leftDown = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), y, leftDown);

        center = translatePoint(flipX * flipY * rawAngle, x, center);
        center = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), y, center);
    }

    public void translatePivot(float x, float y) {
        rightUp = translatePoint(flipX * flipY * rawAngle, -x, rightUp);
        rightUp = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), -y, rightUp);

        rightDown = translatePoint(flipX * flipY * rawAngle, -x, rightDown);
        rightDown = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), -y, rightDown);

        leftUp = translatePoint(flipX * flipY * rawAngle, -x, leftUp);
        leftUp = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), -y, leftUp);

        leftDown = translatePoint(flipX * flipY * rawAngle, -x, leftDown);
        leftDown = translatePoint((float) (flipX * flipY * rawAngle + Math.PI / 2), -y, leftDown);
    }

    public void size(float sizX, float sizY) {
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
        rawSizX = (float) (rawSizX * sizX);
        rawSizY = (float) (rawSizY * sizY);

        rightUp = scalePoint(rawSizX, rawSizY, rightUp);
        rightDown = scalePoint(rawSizX, rawSizY, rightDown);
        leftUp = scalePoint(rawSizX, rawSizY, leftUp);
        leftDown = scalePoint(rawSizX, rawSizY, leftDown);
    }

    public void wholeScale(float size) {
        rightUp.times(size);
        rightDown.times(size);
        leftUp.times(size);
        leftDown.times(size);
        center.times(size);
    }

    public void rotate(float angle) {
        rawAngle += angle;

        rightUp = rotatePoint(angle, rightUp);
        rightDown = rotatePoint(angle, rightDown);
        leftUp = rotatePoint(angle, leftUp);
        leftDown = rotatePoint(angle, leftDown);
    }

    public P rotatePoint(float angle, P point) {
        P res = new P(0f, 0f);

        res.x = (float) (center.x + (Math.cos(angle) * (point.x - center.x)) - (Math.sin(angle) * (point.y - center.y)));
        res.y = (float) (center.y + (Math.cos(angle) * (point.y - center.y)) + (Math.sin(angle) * (point.x - center.x)));

        return res;
    }

    public P scalePoint(float sizX, float sizY, P point) {
        float oldAngle = rawAngle;

        P res = rotatePoint(-oldAngle, point);

        res.x = center.x + (sizX * (res.x - center.x));
        res.y = center.y + (sizY * (res.y - center.y));

        res = rotatePoint(oldAngle, res);

        return res;
    }

    public P translatePoint(float angle, float d, P point) {
        P p = new P(0f, 0f);

        p.x = (float) (point.x + (flipX * d * Math.cos(angle) * rawSizX));
        p.y = (float) (point.y + (flipY * d * Math.sin(angle) * rawSizY));

        return p;
    }

    public P getSize(EPart p) {
        float mi = 1f / p.getModel().ints[0];

        if (p.getFa() == null) {
            return P.newP(p.getSca()).times(mi);
        }

        return getSize(p.getFa()).times(p.getSca()).times(mi);
    }

    public P getBaseSize(EPart p, MaModel model, boolean parent) {
        if(model.confs.length > 0) {
            if(parent) {
                if(p.getFa() != null) {
                    return getBaseSize(p.getFa(), model, true).times(Math.signum(model.parts[p.getInd()][8]), Math.signum(model.parts[p.getInd()][9]));
                } else {
                    return P.newP(Math.signum(model.parts[p.getInd()][8]), Math.signum(model.parts[p.getInd()][9]));
                }
            } else {
                float mi = 1f / model.ints[0];

                if(model.confs[0][0] == -1) {
                    return P.newP(model.parts[0][8] * mi, model.parts[0][9] * mi);
                } else {
                    if(model.confs[0][0] == p.getInd()) {
                        return P.newP(model.parts[model.confs[0][0]][8] * mi, model.parts[model.confs[0][0]][9] * mi);
                    } else {
                        return getBaseSize(p.getParts()[model.confs[0][0]], model, true).times(model.parts[model.confs[0][0]][8] * mi, model.parts[model.confs[0][0]][9] * mi);
                    }
                }
            }
        } else {
            return P.newP(1f, 1f);
        }
    }

    public void apply(EPart p, float size, boolean parent) {
        if(p.opa() <  CommonStatic.getConfig().deadOpa * 0.01 + 1e-5) {
            rightUp = new P(0, 0);
            rightDown = new P(0, 0);
            leftUp = new P(0, 0);
            leftDown = new P(0, 0);
            center = new P(0, 0);

            return;
        }

        P siz = new P(1f, 1f);

        if(p.getFa() != null) {
            apply(p.getFa(), 1f, true);
            siz = getSize(p.getFa());
        }

        P tPos = new P(p.getValRaw(4), p.getValRaw(5)).times(siz);

        if(p.getParts()[0] != p) {
            translate(tPos.x, tPos.y);
            size(p.getValRaw(13), p.getValRaw(14));
        } else {
            if(p.getModel().confs.length > 0) {
                int[] data = p.getModel().confs[0];
                P p0 = getBaseSize(p, p.getModel(), false);

                P shi = P.newP(data[2], data[3]).times(p0).times(siz);
                P.delete(p0);
                translate(-shi.x, -shi.y);
                P.delete(shi);
            }

            P p0 = getSize(p);
            P p1 = P.newP(p.getValRaw(6), p.getValRaw(7)).times(p0).times(siz);

            translate(p1.x, p1.y);

            P.delete(p0);
            P.delete(p1);
        }

        if(p.getValRaw(11) != 0) {
            rotate((float) (flipX * flipY * Math.PI * 2 * p.getValRaw(11) / p.getModel().ints[1]));
        }

        if(!parent) {
            P piv = P.newP(p.getValRaw(6), p.getValRaw(7));
            P scale = getSize(p);

            translatePivot(piv.x, piv.y);
            finalSize(scale.x, scale.y);
            wholeScale(size);

            P.delete(piv);
            P.delete(scale);
        }
    }

    public int[][] getRect() {
        int[][] rect = new int[4][2];

        rect[0][0] = Math.round(leftUp.x);
        rect[0][1] = Math.round(leftUp.y);
        rect[1][0] = Math.round(rightUp.x);
        rect[1][1] = Math.round(rightUp.y);
        rect[2][0] = Math.round(rightDown.x);
        rect[2][1] = Math.round(rightDown.y);
        rect[3][0] = Math.round(leftDown.x);
        rect[3][1] = Math.round(leftDown.y);

        return rect;
    }
}
