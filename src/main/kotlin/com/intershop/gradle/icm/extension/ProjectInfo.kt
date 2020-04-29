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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Extension for server info properties.
 */
open class ProjectInfo @Inject constructor(objectFactory: ObjectFactory ) {

    /**
     * Provider for product id property.
     */
    val productIDProvider: Provider<String>
        get() = productID

    /**
     * Product id of the project.
     */
    val productID: Property<String> = objectFactory.property(String::class.java)

    /**
     * Provider for product name property.
     */
    val productNameProvider: Provider<String>
        get() = productName

    /**
     * Product name of the project.
     */
    val productName: Property<String> = objectFactory.property(String::class.java)

    /**
     * Provider for copyright owner property.
     */
    val copyrightOwnerProvider: Provider<String>
        get() = copyrightOwner

    /**
     * Copyright owner of the project.
     */
    val copyrightOwner: Property<String> = objectFactory.property(String::class.java)

    /**
     * Copyright "from" property of the project.
     */
    val copyrightFromProvider: Provider<String>
        get() = copyrightFrom

    val copyrightFrom: Property<String> = objectFactory.property(String::class.java)

    /**
     * Provider for organization property.
     */
    val organizationProvider: Provider<String>
        get() = organization

    /**
     * Organization of the project.
     */
    val organization: Property<String> = objectFactory.property(String::class.java)

    init {
        productID.convention("ICM")
        productName.convention("Intershop Commerce Management 7")
        copyrightOwner.convention("Intershop Communications")
        copyrightFrom.convention("2005")
        organization.convention("Intershop Communications")
    }
}
