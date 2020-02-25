/*
 * Copyright 2019 Intershop Communications AG.
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
 *
 */
package com.intershop.gradle.icm.extension

import com.intershop.gradle.icm.utils.getValue
import com.intershop.gradle.icm.utils.setValue
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Extension for server info properties.
 */
abstract class ProjectInfo {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val productIDProperty: Property<String> = objectFactory.property(String::class.java)
    private val productNameProperty: Property<String> = objectFactory.property(String::class.java)
    private val copyrightOwnerProperty: Property<String> = objectFactory.property(String::class.java)
    private val copyrightFromProperty: Property<String> = objectFactory.property(String::class.java)
    private val organizationProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        productIDProperty.set("ICM")
        productNameProperty.set("Intershop Commerce Management 7")
        copyrightOwnerProperty.set("Intershop Communications")
        copyrightFromProperty.set("2005")
        organizationProperty.set("Intershop Communications")
    }

    /**
     * Provider for product id property.
     */
    val productIDProvider: Provider<String>
        get() = productIDProperty

    /**
     * Product id of the project.
     */
    var productID by productIDProperty

    /**
     * Provider for product name property.
     */
    val productNameProvider: Provider<String>
        get() = productNameProperty

    /**
     * Product name of the project.
     */
    var productName by productNameProperty

    /**
     * Provider for copyright owner property.
     */
    val copyrightOwnerProvider: Provider<String>
        get() = copyrightOwnerProperty

    /**
     * Copyright owner of the project.
     */
    var copyrightOwner by copyrightOwnerProperty

    /**
     * Copyright "from" property of the project.
     */
    val copyrightFromProvider: Provider<String>
        get() = copyrightFromProperty

    var copyrightFrom by copyrightFromProperty

    /**
     * Provider for organization property.
     */
    val organizationProvider: Provider<String>
        get() = organizationProperty

    /**
     * Organization of the project.
     */
    var organization by organizationProperty
}
