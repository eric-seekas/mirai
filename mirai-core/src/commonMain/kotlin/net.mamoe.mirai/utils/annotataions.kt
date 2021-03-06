/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.utils

import kotlin.annotation.AnnotationTarget.*

/**
 * 标记为一个仅供 Mirai 内部使用的 API.
 *
 * 这些 API 可能会在任意时刻更改, 且不会发布任何预警.
 * 非常不建议在发行版本中使用这些 API.
 */
@Retention(AnnotationRetention.SOURCE)
@Experimental(level = Experimental.Level.ERROR)
@Target(
    CLASS, TYPEALIAS, FUNCTION, PROPERTY, FIELD, CONSTRUCTOR,
    CLASS,
    FUNCTION,
    PROPERTY
)
annotation class MiraiInternalAPI(
    val message: String = ""
)

/**
 * 标记这个类, 类型, 函数, 属性, 字段, 或构造器为实验性的 API.
 *
 * 这些 API 不具有稳定性, 且可能会在任意时刻更改.
 * 不建议在发行版本中使用这些 API.
 */
@Retention(AnnotationRetention.SOURCE)
@Experimental(level = Experimental.Level.WARNING)
@Target(CLASS, TYPEALIAS, FUNCTION, PROPERTY, FIELD, CONSTRUCTOR)
annotation class MiraiExperimentalAPI(
    val message: String = ""
)

/**
 * 标记这个类, 类型, 函数, 属性, 字段, 或构造器为仅供调试阶段使用的.
 *
 * 这些 API 不具有稳定性, 可能会在任意时刻更改, 并且效率非常低下.
 * 非常不建议在发行版本中使用这些 API.
 */
@Retention(AnnotationRetention.SOURCE)
@Experimental(level = Experimental.Level.WARNING)
@Target(CLASS, TYPEALIAS, FUNCTION, PROPERTY, FIELD, CONSTRUCTOR)
annotation class MiraiDebugAPI(
    val message: String = ""
)

/**
 * 标记一个自 Mirai 某个版本起才支持的 API.
 */
@Target(CLASS, PROPERTY, FIELD, CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class SinceMirai(val version: String)