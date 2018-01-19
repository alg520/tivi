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

package me.banes.chris.tivi.trakt.calls

import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.enums.Extended
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import me.banes.chris.tivi.calls.Call
import me.banes.chris.tivi.data.entities.TiviShow
import me.banes.chris.tivi.extensions.toRxSingle
import me.banes.chris.tivi.tmdb.TmdbShowFetcher
import me.banes.chris.tivi.trakt.TraktShowFetcher
import me.banes.chris.tivi.util.AppRxSchedulers
import javax.inject.Inject

class ShowRelatedCall @Inject constructor(
        private val trakt: TraktV2,
        private val traktShowFetcher: TraktShowFetcher,
        private val tmdbShowFetcher: TmdbShowFetcher,
        private val schedulers: AppRxSchedulers
) : Call<Long, List<TiviShow>> {

    private val related = BehaviorSubject.createDefault<List<TiviShow>>(emptyList())

    override fun refresh(param: Long): Completable {
        return trakt.shows().related(param.toString(), 1, 10, Extended.NOSEASONS).toRxSingle()
                .subscribeOn(schedulers.network)
                .observeOn(schedulers.database)
                .toFlowable()
                .flatMapIterable { it }
                .flatMapMaybe { traktObject -> traktShowFetcher.getShow(traktObject.ids.trakt, traktObject) }
                .flatMapSingle {
                    if (it.needsUpdateFromTmdb()) {
                        tmdbShowFetcher.getShow(it.tmdbId!!)
                    } else {
                        Single.just(it)
                    }
                }
                .toList()
                .doOnSuccess(related::onNext)
                .doOnError(related::onError)
                .toCompletable()
    }

    override fun data(param: Long): Flowable<List<TiviShow>> {
        return related.toFlowable(BackpressureStrategy.LATEST)
    }
}
