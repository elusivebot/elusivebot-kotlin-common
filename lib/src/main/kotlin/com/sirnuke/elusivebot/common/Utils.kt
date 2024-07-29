/**
 * Various misc utility functions.
 */

package com.sirnuke.elusivebot.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Extension function for constructing a logger instance on a class. Intended to be used with companion objects.
 *
 * @return Lazy initializer for extended class's specific logger
 */
fun <R : Any> R.logger(): Lazy<Logger> = lazy { LoggerFactory.getLogger(this::class.java) }
