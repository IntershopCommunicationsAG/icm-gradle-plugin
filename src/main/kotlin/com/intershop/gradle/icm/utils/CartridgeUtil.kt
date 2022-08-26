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

package com.intershop.gradle.icm.utils

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.query.ArtifactResolutionQuery
import org.gradle.api.artifacts.result.ArtifactResolutionResult
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Object class for handling of remote cartridge artifacts.
 */
object CartridgeUtil {

    fun isModuleDependency(cartridge: String): Boolean {
        val count = cartridge.split(":").size

        if( count == 3 ) {
            return true
        }

        if( count == 1 ) {
            return false
        }

        throw GradleException("There is a wrong cartridge definition: '${cartridge}'." +
                " Only the project name or a complete module dependency 'group:module:version' is allowed.")
    }

    /**
     *  Calculates id / base file name from module dependency pf a file.
     *
     *  @param identifier module identifier of the dependency
     */
    fun getFileIDFrom(identifier: ModuleComponentIdentifier): String {
        return "${identifier.group}-${identifier.module}-${identifier.version}"
    }

    /**
     * Returns true, if dependency is a external cartridge.
     *
     * @param project    the root project
     * @param identifier module identifier of the dependency
     */
    fun isCartridge(project: Project,
                    identifier: ModuleComponentIdentifier): Boolean {
        return isCartridge(project, identifier.group, identifier.module, identifier.version, listOf())
    }

    /**
     * Returns true, if dependency is a external cartridge and the style
     * is a prdocution like cartridge. This is only checked if style is specified
     *
     * @param project    the root project
     * @param dependency cartridge dependency
     * @param environmentTypes list of environment types
     */
    fun isCartridge(project: Project,
                    dependency: ExternalModuleDependency,
                    environmentTypes: List<EnvironmentType>) : Boolean {
        return isCartridge(project, dependency.group!!, dependency.name, dependency.version!!, environmentTypes)
    }

    /**
     * Returns true, if dependency is a external cartridge and the style
     * is a prdocution like cartridge. This is only checked if style is specified
     *
     * @param project    the root project
     * @param group      module group of the external cartridge
     * @param module     module name of the external cartridge
     * @param version    version of the external cartridge
     * @param environmentTypes list of environment types
     */
    fun isCartridge(project: Project,
                    group: String, module: String, version: String,
                    environmentTypes: List<EnvironmentType>) : Boolean {
        var returnValue = false

        val items = getNodeList(project, group, module, version, getXPath(environmentTypes.isNotEmpty()))

        if(items != null && items.length > 0) {
            returnValue = if (environmentTypes.isNotEmpty()) {
                val style = CartridgeStyle.valueOf(
                    items.item(0).firstChild.nodeValue.uppercase(Locale.getDefault())
                )
                environmentTypes.contains(style.environmentType())
            } else {
                true
            }
        }
        return returnValue
    }

    /**
     * Returns a set of dependencies from a BOM file.
     *
     * @param project    the root project
     * @param moduledep  external module dependency - short notation
     */
    fun getDepenendencySet(project: Project, moduledep: String) : Set<String> {
        val d = moduledep.split(":")
        if( d.size < 3 ) {
            project.logger.warn("This is not a valid external module dependency: '{}'.", moduledep)
        } else {
            return getDepenendencySet(project, d[0], d[1], d[2])
        }
        return mutableSetOf()
    }

    /**
     * Returns a set of dependencies from a BOM file.
     *
     * @param project    the root project
     * @param group      module group of the BOM
     * @param module     module name of the BOM
     * @param version    version of the BOM
     */
    fun getDepenendencySet(project: Project,
                           group: String, module: String, version: String): Set<String> {
        val returnSet = mutableSetOf<String>()

        val items = getNodeList(project, group, module, version,
            "/project/dependencyManagement/dependencies/dependency")

        if(items != null && items.length > 0) {
            for (i in 0 until items.length) {
                val m = getModuleFrom(items.item(i))
                if(m.isNotEmpty()) {
                    returnSet.add(m)
                }
            }
        }

        return returnSet
    }

    fun getCartridgeStyle(project: Project): CartridgeStyle {
        return if (project.hasProperty("cartridge.style")) {
            CartridgeStyle.valueOf(project.property("cartridge.style").toString().uppercase())
        } else {
            CartridgeStyle.CARTRIDGE
        }
    }

    private fun getModuleFrom(node: Node): String {
        if(node.nodeName == "dependency") {
            val moduleNodes = node.childNodes as Element
            val groupId = getContentFrom(moduleNodes.getElementsByTagName("groupId"))
            val artifactId = getContentFrom(moduleNodes.getElementsByTagName("artifactId"))
            val version = getContentFrom(moduleNodes.getElementsByTagName("version"))

            if(groupId.isNotEmpty() && artifactId.isNotEmpty() && version.isNotEmpty()) {
                return "${groupId}:${artifactId}:${version}"
            }
        }
        return ""
    }

    private fun getContentFrom(nodeList: NodeList): String {
        if(nodeList.length > 0) {
            return nodeList.item(0).textContent
        }
        return ""
    }

    private fun getNodeList(project: Project,
                            group: String, module: String, version: String,
                            xpath: String): NodeList? {

        val query: ArtifactResolutionQuery = project.dependencies
            .createArtifactResolutionQuery()
            .forModule(group, module, version)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)

        val result: ArtifactResolutionResult = query.execute()

        result.resolvedComponents.forEach { component ->
            val mavenPomArtifacts: Set<ArtifactResult> = component.getArtifacts(MavenPomArtifact::class.java)
            val pomArtifact = mavenPomArtifacts.find {
                it is ResolvedArtifactResult && it.file.name == "${module}-${version}.pom"
            }

            if (pomArtifact != null) {
                val modulePomArtifact = pomArtifact as ResolvedArtifactResult
                try {
                    val doc = readXML(modulePomArtifact.file)
                    val xpFactory = XPathFactory.newInstance()
                    val xPath = xpFactory.newXPath()

                    return xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList
                } catch (ex: Exception) {
                    project.logger.info("Pom file is not readable - {}:{}:{}", group, module, version)
                }
            }
        }
        return null
    }

    private fun getXPath(forStyle: Boolean): String {
        return if(forStyle) {
            "/project/properties/cartridge.style"
        } else {
            "/project/properties/cartridge.name"
        }
    }

    private fun readXML(xmlFile: File): Document {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(xmlFile.readText()))

        return dBuilder.parse(xmlInput)
    }
}
