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

import android.animation.ObjectAnimator
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.support.v7.graphics.Palette
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_show_details.*
import me.banes.chris.tivi.R
import me.banes.chris.tivi.SharedElementHelper
import me.banes.chris.tivi.TiviFragment
import me.banes.chris.tivi.data.entities.TiviShow
import me.banes.chris.tivi.extensions.doWhenLaidOut
import me.banes.chris.tivi.extensions.observeK
import me.banes.chris.tivi.home.HomeNavigator
import me.banes.chris.tivi.home.HomeNavigatorViewModel
import me.banes.chris.tivi.ui.GlidePaletteListener
import me.banes.chris.tivi.ui.NoopApplyWindowInsetsListener
import me.banes.chris.tivi.ui.RoundRectViewOutline
import me.banes.chris.tivi.ui.SpacingItemDecorator
import me.banes.chris.tivi.ui.transitions.DrawableAlphaProperty
import me.banes.chris.tivi.util.ScrimUtil
import javax.inject.Inject

class ShowDetailsFragment : TiviFragment() {

    companion object {
        private const val KEY_SHOW_ID = "show_id"

        fun create(id: Long): ShowDetailsFragment {
            return ShowDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(KEY_SHOW_ID, id)
                }
            }
        }
    }

    private val callbacks = object : ShowDetailsEpoxyController.Callbacks {
        override fun onItemClicked(item: TiviShow) {
            val sharedElementHelper = SharedElementHelper()
            addSharedElementEntry(item, sharedElementHelper, "poster")
            viewModel.onItemPostedClicked(homeNavigator, item, sharedElementHelper)
        }

        private fun addSharedElementEntry(
                show: TiviShow,
                sharedElementHelper: SharedElementHelper,
                transitionName: String? = show.homepage
        ) {
            details_rv.findViewHolderForItemId(show.traktId!!.toLong())?.let {
                sharedElementHelper.addSharedElement(it.itemView, transitionName)
            }
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var homeNavigator: HomeNavigator

    private lateinit var viewModel: ShowDetailsFragmentViewModel
    private lateinit var epoxyController: ShowDetailsEpoxyController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ShowDetailsFragmentViewModel::class.java)
        homeNavigator = ViewModelProviders.of(activity!!, viewModelFactory).get(HomeNavigatorViewModel::class.java)

        arguments?.let {
            viewModel.showId = it.getLong(KEY_SHOW_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_show_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        epoxyController = ShowDetailsEpoxyController(view.context, callbacks)
        details_rv.setController(epoxyController)
        details_rv.addItemDecoration(SpacingItemDecorator(details_rv.paddingTop))

        details_backdrop.setOnApplyWindowInsetsListener(NoopApplyWindowInsetsListener)

        details_poster.clipToOutline = true
        details_poster.outlineProvider = RoundRectViewOutline
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.show.observeK(this) { update() }
        viewModel.relatedShows.observeK(this) { update() }
        viewModel.tmdbImageUrlProvider.observeK(this) { update() }
    }

    private fun onBackdropPaletteLoaded(palette: Palette) {
        palette.dominantSwatch?.let {
            val background = ColorDrawable(it.rgb)
            details_coordinator.background = background
            ObjectAnimator.ofInt(background, DrawableAlphaProperty, 0, 255).start()

            val scrim = ScrimUtil.makeCubicGradientScrimDrawable(it.rgb, 10, Gravity.BOTTOM)
            val drawable = LayerDrawable(arrayOf(scrim)).apply {
                setLayerGravity(0, Gravity.FILL)
                setLayerInsetTop(0, details_backdrop.height / 2)
            }
            details_backdrop.foreground = drawable
            ObjectAnimator.ofInt(drawable, DrawableAlphaProperty, 0, 255).start()
        }
    }

    private fun update() {
        val show = viewModel.show.value
        val relatedShows = viewModel.relatedShows.value
        val imageProvider = viewModel.tmdbImageUrlProvider.value

        show?.tmdbBackdropPath?.let { path ->
            details_backdrop.doWhenLaidOut {
                Glide.with(this)
                        .load(imageProvider?.getBackdropUrl(path, details_backdrop.width))
                        .listener(GlidePaletteListener(this::onBackdropPaletteLoaded))
                        .into(details_backdrop)
            }
        }

        show?.tmdbPosterPath?.let { path ->
            details_poster.doWhenLaidOut {
                Glide.with(this)
                        .load(imageProvider?.getPosterUrl(path, details_poster.width))
                        .into(details_poster)
            }
        }

        epoxyController.show = show
        epoxyController.relatedShows = relatedShows
        epoxyController.imageProvider = imageProvider

        scheduleStartPostponedTransitions()
    }
}