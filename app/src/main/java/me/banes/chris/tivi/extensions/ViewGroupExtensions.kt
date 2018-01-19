/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.banes.chris.tivi.extensions

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent

private val localMatrix = ThreadLocal<Matrix>()
    get() {
        val value = field.get()
        if (value == null) {
            field.set(Matrix())
        }
        return field
    }

private val localRectF = ThreadLocal<RectF>()
    get() {
        val value = field.get()
        if (value == null) {
            field.set(RectF())
        }
        return field
    }

/**
 * This is a port of the common [ViewGroup.offsetDescendantRectToMyCoords]
 * from the framework, but adapted to take transformations into account. The result
 * will be the bounding rect of the real transformed rect.
 *
 * @param descendant view defining the original coordinate system of rect
 * @param rect (in/out) the rect to offset from descendant to this view's coordinate system
 */
fun ViewGroup.offsetDescendantRect(descendant: View, rect: Rect) {
    val m = localMatrix.get()
    m.reset()

    offsetDescendantMatrix(parent, descendant, m)

    val rectF: RectF = localRectF.get()
    rectF.set(rect)
    m.mapRect(rectF)

    rect.set((rectF.left + 0.5f).toInt(), (rectF.top + 0.5f).toInt(),
            (rectF.right + 0.5f).toInt(), (rectF.bottom + 0.5f).toInt())
}

/**
 * Retrieve the transformed bounding rect of an arbitrary descendant view.
 * This does not need to be a direct child.
 *
 * @param descendant descendant view to reference
 * @param out rect to set to the bounds of the descendant view
 */
fun ViewGroup.getDescendantViewRect(descendant: View, out: Rect) {
    out.set(0, 0, descendant.width, descendant.height)
    offsetDescendantRect(descendant, out)
}

private fun offsetDescendantMatrix(target: ViewParent, view: View, m: Matrix) {
    val parent = view.parent
    if (parent is View && parent !== target) {
        val vp = parent as View
        offsetDescendantMatrix(target, vp, m)
        m.preTranslate((-vp.scrollX).toFloat(), (-vp.scrollY).toFloat())
    }

    m.preTranslate(view.left.toFloat(), view.top.toFloat())

    if (!view.matrix.isIdentity) {
        m.preConcat(view.matrix)
    }
}
