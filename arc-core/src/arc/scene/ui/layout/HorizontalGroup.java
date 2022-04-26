package arc.scene.ui.layout;

import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;

/**
 * A group that lays out its children side by side horizontally, with optional wrapping. This can be easier than using
 * {@link Table} when elements need to be inserted into or removed from the middle of the group.
 * <p>
 * The preferred width is the sum of the children's preferred widths plus spacing. The preferred height is the largest preferred
 * height of any child. The preferred size is slightly different when {@link #wrap() wrap} is enabled. The min size is the
 * preferred size and the max size is 0.
 * <p>
 * Widgets are sized using their {@link Layout#getPrefWidth() preferred width}, so widgets which return 0 as their preferred width
 * will be given a width of 0 (eg, a label with {@link Label#setWrap(boolean) word wrap} enabled).
 * @author Nathan Sweet
 */
public class HorizontalGroup extends WidgetGroup{
    private float prefWidth, prefHeight, lastPrefHeight;
    private boolean sizeInvalid = true;
    private FloatSeq rowSizes; // row width, row height, ...

    private int align = Align.left, rowAlign;
    private boolean reverse, round = true, wrap, expand;
    private float space, wrapSpace, fill, padTop, padLeft, padBottom, padRight;

    public HorizontalGroup(){
        this.touchable = Touchable.childrenOnly;
    }

    @Override
    public void invalidate(){
        super.invalidate();
        sizeInvalid = true;
    }

    private void computeSize(){
        sizeInvalid = false;
        SnapshotSeq<Element> children = getChildren();
        int n = children.size;
        prefHeight = 0;
        if(wrap){
            prefWidth = 0;
            if(rowSizes == null)
                rowSizes = new FloatSeq();
            else
                rowSizes.clear();
            FloatSeq rowSizes = this.rowSizes;
            float space = this.space, wrapSpace = this.wrapSpace;
            float pad = padLeft + padRight, groupWidth = getWidth() - pad, x = 0, y = 0, rowHeight = 0;
            int i = 0, incr = 1;
            if(reverse){
                i = n - 1;
                n = -1;
                incr = -1;
            }
            for(; i != n; i += incr){
                Element child = children.get(i);

                float width, height;
                if(child != null){
                    width = (child).getPrefWidth();
                    height = (child).getPrefHeight();
                }else{
                    width = child.getWidth();
                    height = child.getHeight();
                }

                float incrX = width + (x > 0 ? space : 0);
                if(x + incrX > groupWidth && x > 0){
                    rowSizes.add(x);
                    rowSizes.add(rowHeight);
                    prefWidth = Math.max(prefWidth, x + pad);
                    if(y > 0) y += wrapSpace;
                    y += rowHeight;
                    rowHeight = 0;
                    x = 0;
                    incrX = width;
                }
                x += incrX;
                rowHeight = Math.max(rowHeight, height);
            }
            rowSizes.add(x);
            rowSizes.add(rowHeight);
            prefWidth = Math.max(prefWidth, x + pad);
            if(y > 0) y += wrapSpace;
            prefHeight = Math.max(prefHeight, y + rowHeight);
        }else{
            prefWidth = padLeft + padRight + space * (n - 1);
            for(int i = 0; i < n; i++){
                Element child = children.get(i);
                if(child != null){
                    prefWidth += (child).getPrefWidth();
                    prefHeight = Math.max(prefHeight, (child).getPrefHeight());
                }else{
                    prefWidth += child.getWidth();
                    prefHeight = Math.max(prefHeight, child.getHeight());
                }
            }
        }
        prefHeight += padTop + padBottom;
        if(round){
            prefWidth = Math.round(prefWidth);
            prefHeight = Math.round(prefHeight);
        }
    }

    @Override
    public void layout(){
        if(sizeInvalid) computeSize();

        if(wrap){
            layoutWrapped();
            return;
        }

        boolean round = this.round;
        int align = this.align;
        float space = this.space, padBottom = this.padBottom, fill = this.fill;
        float rowHeight = (expand ? getHeight() : prefHeight) - padTop - padBottom, x = padLeft;

        if((align & Align.right) != 0)
            x += getWidth() - prefWidth;
        else if((align & Align.left) == 0) // center
            x += (getWidth() - prefWidth) / 2;

        float startY;
        if((align & Align.bottom) != 0)
            startY = padBottom;
        else if((align & Align.top) != 0)
            startY = getHeight() - padTop - rowHeight;
        else
            startY = padBottom + (getHeight() - padBottom - padTop - rowHeight) / 2;

        align = rowAlign;

        SnapshotSeq<Element> children = getChildren();
        int i = 0, n = children.size, incr = 1;
        if(reverse){
            i = n - 1;
            n = -1;
            incr = -1;
        }
        for(; i != n; i += incr){
            Element child = children.get(i);

            float width, height;
            width = child.getPrefWidth();
            height = child.getPrefHeight();


            if(fill > 0) height = rowHeight * fill;

            height = Math.max(height, child.getMinHeight());
            float maxHeight = child.getMaxHeight();
            if(maxHeight > 0 && height > maxHeight) height = maxHeight;

            float y = startY;
            if((align & Align.top) != 0)
                y += rowHeight - height;
            else if((align & Align.bottom) == 0) // center
                y += (rowHeight - height) / 2;

            if(round)
                child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            else
                child.setBounds(x, y, width, height);
            x += width + space;

            child.validate();
        }
    }

    private void layoutWrapped(){
        float prefHeight = getPrefHeight();
        if(prefHeight != lastPrefHeight){
            lastPrefHeight = prefHeight;
            invalidateHierarchy();
        }

        int align = this.align;
        boolean round = this.round;
        float space = this.space, fill = this.fill, wrapSpace = this.wrapSpace;
        float maxWidth = prefWidth - padLeft - padRight;
        float rowY = prefHeight - padTop, groupWidth = getWidth(), xStart = padLeft, x = 0, rowHeight = 0;

        if((align & Align.top) != 0)
            rowY += getHeight() - prefHeight;
        else if((align & Align.bottom) == 0) // center
            rowY += (getHeight() - prefHeight) / 2;

        if((align & Align.right) != 0)
            xStart += groupWidth - prefWidth;
        else if((align & Align.left) == 0) // center
            xStart += (groupWidth - prefWidth) / 2;

        groupWidth -= padRight;
        align = this.rowAlign;

        FloatSeq rowSizes = this.rowSizes;
        SnapshotSeq<Element> children = getChildren();
        int i = 0, n = children.size, incr = 1;
        if(reverse){
            i = n - 1;
            n = -1;
            incr = -1;
        }
        for(int r = 0; i != n; i += incr){
            Element child = children.get(i);

            float width, height;
            width = child.getPrefWidth();
            height = child.getPrefHeight();


            if(x + width > groupWidth || r == 0){
                x = xStart;
                if((align & Align.right) != 0)
                    x += maxWidth - rowSizes.get(r);
                else if((align & Align.left) == 0) // center
                    x += (maxWidth - rowSizes.get(r)) / 2;
                rowHeight = rowSizes.get(r + 1);
                if(r > 0) rowY -= wrapSpace;
                rowY -= rowHeight;
                r += 2;
            }

            if(fill > 0) height = rowHeight * fill;

            height = Math.max(height, child.getMinHeight());
            float maxHeight = child.getMaxHeight();
            if(maxHeight > 0 && height > maxHeight) height = maxHeight;

            float y = rowY;
            if((align & Align.top) != 0)
                y += rowHeight - height;
            else if((align & Align.bottom) == 0) // center
                y += (rowHeight - height) / 2;

            if(round)
                child.setBounds(Math.round(x), Math.round(y), Math.round(width), Math.round(height));
            else
                child.setBounds(x, y, width, height);
            x += width + space;

            child.validate();
        }
    }

    @Override
    public float getPrefWidth(){
        if(wrap) return 0;
        if(sizeInvalid) computeSize();
        return prefWidth;
    }

    @Override
    public float getPrefHeight(){
        if(sizeInvalid) computeSize();
        return prefHeight;
    }

    /** If true (the default), positions and sizes are rounded to integers. */
    public void setRound(boolean round){
        this.round = round;
    }

    /** The children will be displayed last to first. */
    public HorizontalGroup reverse(){
        this.reverse = true;
        return this;
    }

    /** If true, the children will be displayed last to first. */
    public HorizontalGroup reverse(boolean reverse){
        this.reverse = reverse;
        return this;
    }

    public boolean getReverse(){
        return reverse;
    }

    /** Sets the horizontal space between children. */
    public HorizontalGroup space(float space){
        this.space = space;
        return this;
    }

    public float getSpace(){
        return space;
    }

    /** Sets the vertical space between rows when wrap is enabled. */
    public HorizontalGroup wrapSpace(float wrapSpace){
        this.wrapSpace = wrapSpace;
        return this;
    }

    public float getWrapSpace(){
        return wrapSpace;
    }

    /** Sets the marginTop, marginLeft, marginBottom, and marginRight to the specified value. */
    public HorizontalGroup pad(float pad){
        padTop = pad;
        padLeft = pad;
        padBottom = pad;
        padRight = pad;
        return this;
    }

    public HorizontalGroup pad(float top, float left, float bottom, float right){
        padTop = top;
        padLeft = left;
        padBottom = bottom;
        padRight = right;
        return this;
    }

    public HorizontalGroup padTop(float padTop){
        this.padTop = padTop;
        return this;
    }

    public HorizontalGroup padLeft(float padLeft){
        this.padLeft = padLeft;
        return this;
    }

    public HorizontalGroup padBottom(float padBottom){
        this.padBottom = padBottom;
        return this;
    }

    public HorizontalGroup padRight(float padRight){
        this.padRight = padRight;
        return this;
    }

    public float getPadTop(){
        return padTop;
    }

    public float getPadLeft(){
        return padLeft;
    }

    public float getPadBottom(){
        return padBottom;
    }

    public float getPadRight(){
        return padRight;
    }

    /**
     * Sets the alignment of all widgets within the horizontal group. Set to {@link Align#center}, {@link Align#top},
     * {@link Align#bottom}, {@link Align#left}, {@link Align#right}, or any combination of those.
     */
    public HorizontalGroup align(int align){
        this.align = align;
        return this;
    }

    /** Sets the alignment of all widgets within the horizontal group to {@link Align#center}. This clears any other alignment. */
    public HorizontalGroup center(){
        align = Align.center;
        return this;
    }

    /** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of all widgets within the horizontal group. */
    public HorizontalGroup top(){
        align |= Align.top;
        align &= ~Align.bottom;
        return this;
    }

    /** Adds {@link Align#left} and clears {@link Align#right} for the alignment of all widgets within the horizontal group. */
    public HorizontalGroup left(){
        align |= Align.left;
        align &= ~Align.right;
        return this;
    }

    /** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of all widgets within the horizontal group. */
    public HorizontalGroup bottom(){
        align |= Align.bottom;
        align &= ~Align.top;
        return this;
    }

    /** Adds {@link Align#right} and clears {@link Align#left} for the alignment of all widgets within the horizontal group. */
    public HorizontalGroup right(){
        align |= Align.right;
        align &= ~Align.left;
        return this;
    }

    public int getAlign(){
        return align;
    }

    public HorizontalGroup fill(){
        fill = 1f;
        return this;
    }

    /** @param fill 0 will use preferred width. */
    public HorizontalGroup fill(float fill){
        this.fill = fill;
        return this;
    }

    public float getFill(){
        return fill;
    }

    public HorizontalGroup expand(){
        expand = true;
        return this;
    }

    /** When true and wrap is false, the rows will take up the entire horizontal group height. */
    public HorizontalGroup expand(boolean expand){
        this.expand = expand;
        return this;
    }

    public boolean getExpand(){
        return expand;
    }

    /** Sets fill to 1 and expand to true. */
    public HorizontalGroup grow(){
        expand = true;
        fill = 1;
        return this;
    }

    /**
     * If false, the widgets are arranged in a single row and the preferred width is the widget widths plus spacing. If true, the
     * widgets will wrap using the width of the horizontal group. The preferred width of the group will be 0 as it is expected that
     * something external will set the width of the group. Default is false.
     * <p>
     * When wrap is enabled, the group's preferred height depends on the width of the group. In some cases the parent of the group
     * will need to layout twice: once to set the width of the group and a second time to adjust to the group's new preferred
     * height.
     */
    public HorizontalGroup wrap(){
        wrap = true;
        return this;
    }

    public HorizontalGroup wrap(boolean wrap){
        this.wrap = wrap;
        return this;
    }

    public boolean getWrap(){
        return wrap;
    }

    /**
     * Sets the alignment of widgets within each row of the horizontal group. Set to {@link Align#center}, {@link Align#top}, or
     * {@link Align#bottom}.
     */
    public HorizontalGroup rowAlign(int row){
        this.rowAlign = row;
        return this;
    }

    /** Sets the alignment of widgets within each row to {@link Align#center}. This clears any other alignment. */
    public HorizontalGroup rowCenter(){
        rowAlign = Align.center;
        return this;
    }

    /** Sets {@link Align#top} and clears {@link Align#bottom} for the alignment of widgets within each row. */
    public HorizontalGroup rowTop(){
        rowAlign |= Align.top;
        rowAlign &= ~Align.bottom;
        return this;
    }

    /** Sets {@link Align#bottom} and clears {@link Align#top} for the alignment of widgets within each row. */
    public HorizontalGroup rowBottom(){
        rowAlign |= Align.bottom;
        rowAlign &= ~Align.top;
        return this;
    }
}