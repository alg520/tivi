/*
 * Copyright 2017 Google, Inc.
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

package me.banes.chris.tivi.details

import android.content.Context
import android.graphics.Rect
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import me.banes.chris.tivi.R
import me.banes.chris.tivi.extensions.getDescendantViewRect

class DetailPosterBehavior(context: Context, attrs: AttributeSet?) : CoordinatorLayout.Behavior<ImageView>(context, attrs) {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: ImageView, dependency: View): Boolean {
        return dependency is RecyclerView
    }

    override fun onLayoutChild(parent: CoordinatorLayout, poster: ImageView, layoutDirection: Int): Boolean {
        // Let the parent layout the poster first
        parent.onLayoutChild(poster, layoutDirection)

        offsetPoster(parent, poster, parent.findViewById(R.id.details_rv))
        return true
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, poster: ImageView, dependency: View): Boolean {
        offsetPoster(parent, poster, dependency as RecyclerView)
        return true
    }

    private fun offsetPoster(parent: CoordinatorLayout, poster: ImageView, recyclerView: RecyclerView) {
        val posterLp = poster.layoutParams as CoordinatorLayout.LayoutParams

        val firstChildItemRv = recyclerView.findViewHolderForAdapterPosition(0)
        if (firstChildItemRv != null) {
            val childRect = Rect()
            parent.getDescendantViewRect(firstChildItemRv.itemView, childRect)

            ViewCompat.offsetLeftAndRight(poster, posterLp.marginStart + childRect.left - poster.left)
            ViewCompat.offsetTopAndBottom(poster, childRect.bottom - poster.bottom)

            if (recyclerView.top < childRect.height()) {
                // shrink
                //ObjectAnimator.ofFloa
            } else {
                // grow
            }
        }
    }
}