/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.zipline

import app.cash.zipline.testing.SealedClassMessageService
import app.cash.zipline.testing.SealedMessage.BlueMessage
import app.cash.zipline.testing.SealedMessage.RedMessage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SealedClassSerializersTest {
  @Rule @JvmField val ziplineTestRule = ZiplineTestRule()
  private val dispatcher = ziplineTestRule.dispatcher
  private val zipline = Zipline.create(dispatcher)

  @Before fun setUp(): Unit = runBlocking(dispatcher) {
    zipline.loadTestingJs()
  }

  @After fun tearDown() = runBlocking(dispatcher) {
    zipline.close()
  }

  @Test fun sealedClassesEncodeAndDecode() = runBlocking(dispatcher) {
    val service = zipline.take<SealedClassMessageService>("sealedClassMessageService")
    zipline.quickJs.evaluate(
      "testing.app.cash.zipline.testing.prepareSealedClassMessageService()"
    )

    assertThat(service.colorSwap(RedMessage("hello!")))
      .isEqualTo(BlueMessage("hello!"))
  }

  /**
   * We recently had a bug where JSON use inside of flows didn't use `useArrayPolymorphism = true`,
   * which prevented us from decoding what was encoded.
   */
  @Test fun sealedClassesFlow(): Unit = runBlocking(dispatcher) {
    val service = zipline.take<SealedClassMessageService>("sealedClassMessageService")
    zipline.quickJs.evaluate(
      "testing.app.cash.zipline.testing.prepareSealedClassMessageService()"
    )

    val sealedMessageFlow = flow {
      emit(RedMessage("a"))
      emit(BlueMessage("b"))
      emit(RedMessage("c"))
    }
    val swappedFlow = service.colorSwapFlow(sealedMessageFlow)

    assertThat(swappedFlow.toList()).containsExactly(
      BlueMessage("a"),
      RedMessage("b"),
      BlueMessage("c"),
    )
  }
}
