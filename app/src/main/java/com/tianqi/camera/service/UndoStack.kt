package com.tianqi.camera.service

/** 撤销/重做栈：快照式，所有图层操作（增删、变换、涂鸦）都经过 push */
class UndoStack<T>(initial: T, private val limit: Int = 50) {
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()

    var current: T = initial
        private set

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun push(next: T) {
        past.addLast(current)
        if (past.size > limit) past.removeFirst()
        future.clear()
        current = next
    }

    fun undo(): T {
        if (!canUndo) return current
        future.addLast(current)
        current = past.removeLast()
        return current
    }

    fun redo(): T {
        if (!canRedo) return current
        past.addLast(current)
        current = future.removeLast()
        return current
    }
}
