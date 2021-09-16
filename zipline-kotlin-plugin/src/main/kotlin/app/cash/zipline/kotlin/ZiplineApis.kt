/*
 * Copyright (C) 2021 Square, Inc.
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
package app.cash.zipline.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName

/** Looks up APIs used by the code rewriters. */
internal class ZiplineApis(
  private val pluginContext: IrPluginContext,
) {
  private val packageFqName = FqName("app.cash.zipline")
  private val bridgeFqName = FqName("app.cash.zipline.internal.bridge")
  private val serializationFqName = FqName("kotlinx.serialization")
  private val serializationModulesFqName = FqName("kotlinx.serialization.modules")
  private val ziplineFqName = packageFqName.child("Zipline")
  private val ziplineCompanionFqName = ziplineFqName.child("Companion")
  private val endpointFqName = bridgeFqName.child("Endpoint")

  val any: IrClassSymbol
    get() = pluginContext.referenceClass(FqName("kotlin.Any"))!!

  val kSerializer: IrClassSymbol
    get() = pluginContext.referenceClass(serializationFqName.child("KSerializer"))!!

  val serializerFunction: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(serializationFqName.child("serializer"))
      .single {
        it.owner.extensionReceiverParameter?.type?.classFqName == serializersModuleFqName &&
          it.owner.valueParameters.isEmpty() &&
          it.owner.typeParameters.size == 1
      }

  val serializersModuleFqName = serializationModulesFqName.child("SerializersModule")

  val inboundCall: IrClassSymbol
    get() = pluginContext.referenceClass(bridgeFqName.child("InboundCall"))!!

  val inboundCallParameter: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(bridgeFqName.child("InboundCall").child("parameter"))
      .single()

  val inboundCallResult: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(bridgeFqName.child("InboundCall").child("result"))
      .single()

  val inboundCallUnexpectedFunction: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(
      bridgeFqName.child("InboundCall").child("unexpectedFunction")
    ).single()

  val inboundBridge: IrClassSymbol
    get() = pluginContext.referenceClass(bridgeFqName.child("InboundBridge"))!!

  val inboundBridgeContextFqName = bridgeFqName.child("InboundBridge").child("Context")

  val inboundBridgeContext: IrClassSymbol
    get() = pluginContext.referenceClass(inboundBridgeContextFqName)!!

  val inboundBridgeContextSerializersModule: IrPropertySymbol
    get() = pluginContext.referenceProperties(
      inboundBridgeContextFqName.child("serializersModule")
    ).single()

  val inboundBridgeCreate: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(
      bridgeFqName.child("InboundBridge").child("create")
    ).single()

  val inboundCallHandler: IrClassSymbol
    get() = pluginContext.referenceClass(bridgeFqName.child("InboundCallHandler"))!!

  val inboundCallHandlerCall: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(
      bridgeFqName.child("InboundCallHandler").child("call")
    ).single()

  val inboundCallHandlerCallSuspending: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(
      bridgeFqName.child("InboundCallHandler").child("callSuspending")
    ).single()

  val inboundCallHandlerContext: IrPropertySymbol
    get() = pluginContext.referenceProperties(
      bridgeFqName.child("InboundCallHandler").child("context")
    ).single()

  val outboundCallInvoke: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(bridgeFqName.child("OutboundCall").child("invoke"))
      .single()

  val outboundCallInvokeSuspending: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(
      bridgeFqName.child("OutboundCall").child("invokeSuspending")
    ).single()

  val outboundCallParameter: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(bridgeFqName.child("OutboundCall").child("parameter"))
      .single()

  val outboundBridge: IrClassSymbol
    get() = pluginContext.referenceClass(bridgeFqName.child("OutboundBridge"))!!

  val outboundBridgeContextFqName = bridgeFqName.child("OutboundBridge").child("Context")

  val outboundBridgeContext: IrClassSymbol
    get() = pluginContext.referenceClass(outboundBridgeContextFqName)!!

  val outboundBridgeContextNewCall: IrSimpleFunctionSymbol
    get() = pluginContext.referenceFunctions(
      outboundBridgeContextFqName.child("newCall")
    ).single()

  val outboundBridgeContextSerializersModule: IrPropertySymbol
    get() = pluginContext.referenceProperties(
      outboundBridgeContextFqName.child("serializersModule")
    ).single()

  val outboundBridgeCreate: IrSimpleFunctionSymbol
    get() = outboundBridge.functions.single { it.owner.name.identifier == "create" }

  /** Keys are functions like `Zipline.get()` and values are their rewrite targets. */
  val getRewriteFunctions: Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol> = buildRewritesMap(
    ziplineFqName.child("get"),
    ziplineCompanionFqName.child("get"),
    endpointFqName.child("get"),
  )

  /** Keys are functions like `Zipline.set()` and values are their rewrite targets. */
  val setRewriteFunctions: Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol> = buildRewritesMap(
    ziplineFqName.child("set"),
    ziplineCompanionFqName.child("set"),
    endpointFqName.child("set"),
  )

  /** Maps overloads from the user-friendly function to its internal rewrite target. */
  private fun buildRewritesMap(
    vararg functionNames: FqName
  ): Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol> {
    val result = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()
    for (functionName in functionNames) {
      val overloads = pluginContext.referenceFunctions(functionName)
      if (overloads.isEmpty()) continue // The Companion APIs are JS-only.
      val original = overloads.single {
        it.owner.valueParameters[1].type.classFqName == serializersModuleFqName
      }
      val target = overloads.single { it != original }
      result[original] = target
    }
    return result
  }
}
