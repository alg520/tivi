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

import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.MutableLiveData
import io.reactivex.rxkotlin.plusAssign
import me.banes.chris.tivi.data.entities.TiviShow
import me.banes.chris.tivi.tmdb.TmdbManager
import me.banes.chris.tivi.trakt.calls.ShowDetailsCall
import me.banes.chris.tivi.trakt.calls.ShowRelatedCall
import me.banes.chris.tivi.util.AppRxSchedulers
import me.banes.chris.tivi.util.RxAwareViewModel
import timber.log.Timber
import javax.inject.Inject

class ShowDetailsFragmentViewModel @Inject constructor(
        private val schedulers: AppRxSchedulers,
        private val showCall: ShowDetailsCall,
        private val relatedCall: ShowRelatedCall,
        tmdbManager: TmdbManager
) : RxAwareViewModel() {

    val show = MutableLiveData<TiviShow>()
    val relatedShows = MutableLiveData<List<TiviShow>>()

    val tmdbImageUrlProvider = LiveDataReactiveStreams.fromPublisher(tmdbManager.imageProvider)!!

    var showId: Long? = null
        set(value) {
            if (field != value) {
                field = value
                refresh()
            }
        }

    private fun refresh() {
        disposables.clear()

        showId?.let {
            disposables += relatedCall.data(it)
                    .observeOn(schedulers.main)
                    .subscribe(relatedShows::setValue, Timber::e)

            disposables += showCall.refresh(it)
                    .subscribe(this::onRefreshSuccess, this::onRefreshError)

            disposables += showCall.data(it)
                    .observeOn(schedulers.main)
                    .subscribe({
                        show.value = it
                        disposables += relatedCall.refresh(it.traktId!!.toLong()).subscribe()
                    }, Timber::e)
        }
    }

    private fun onRefreshSuccess() {
        // TODO nothing really to do here
    }

    private fun onRefreshError(t: Throwable) {
        Timber.e(t, "Error while refreshing")
    }
}