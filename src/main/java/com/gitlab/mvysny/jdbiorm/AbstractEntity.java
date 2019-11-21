package com.gitlab.mvysny.jdbiorm;

import java.io.Serializable;

/**
 * An abstract entity which doesn't introduce any methods per se. It is expected
 * that every programming language introduces an entity interface which is best
 * suited for that language.
 * <p></p>
 * It is expected that every entity has at least the methods present in {@link Entity};
 * however, the programming language can use its own native syntax to define those methods.
 * @author mavi
 */
public interface AbstractEntity<ID> extends Serializable {
    // for example the problem with Kotlin is as follows:
    // for Java, Entity.getId() and Entity.setId() are perfectly valid. However,
    // Kotlin no longer can introduce an overridable property id, since
    // abstract var id can't override getters/setters: https://youtrack.jetbrains.com/issue/KT-6653
    // and therefore we would have to override getters/setters in every entity class, which
    // is annoying: https://github.com/mvysny/vok-orm/issues/13
    //
    // if we implemented Entity in Kotlin, you would have to implement save() methods for every Java entity
    // since Kotlin default interface methods aren't compiled to Java default interface methods
    //
    // also, Kotlin can have one save(validate = true) method with default values; then
    // it's clear and obvious which one to override since there's only one.
    // Java entity has to have both save() and save(boolean) and care must be taken to override
    // the save(boolean) only.
}
