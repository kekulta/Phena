package tech.kekulta.phena

import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> T.next(): T {
    return enumEntries<T>()[(this.ordinal + 1) % enumEntries<T>().size]
}
