/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.commons.collection;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for operations on various collections.
 *
 * @author Morten Olav Hansen
 */
public class CollectionUtils
{
    public static final String[] STRING_ARR = new String[0];

    private CollectionUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }

    /**
     * Performs a flat mapping of the given collection using the given mapping
     * function.
     *
     * @param <A>
     * @param <B>
     * @param collection the collection of objects to map.
     * @param mapper the mapping function.
     * @return a set of mapped objects.
     */
    public static <A, B> Set<B> flatMapToSet( Collection<A> collection,
        Function<? super A, ? extends Collection<B>> mapper )
    {
        return collection.stream()
            .map( mapper )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );
    }

    /**
     * Performs a mapping of the given collection using the given mapping
     * function.
     *
     * @param <A>
     * @param <B>
     * @param collection the collection of objects to map.
     * @param mapper the mapping function.
     * @return a set of mapped objects.
     */
    public static <A, B> Set<B> mapToSet( Collection<A> collection, Function<? super A, ? extends B> mapper )
    {
        return collection.stream()
            .map( mapper )
            .collect( Collectors.toSet() );
    }

    /**
     * Performs a mapping of the given collection using the given mapping
     * function.
     *
     * @param <A>
     * @param <B>
     * @param collection the collection of objects to map.
     * @param mapper the mapping function.
     * @return a list of mapped objects.
     */
    public static <A, B> List<B> mapToList( Collection<A> collection, Function<? super A, ? extends B> mapper )
    {
        return collection.stream()
            .map( mapper )
            .collect( toList() );
    }

    /**
     * Returns the first matching item in the given collection based on the
     * given predicate. Returns null if no match is found.
     *
     * @param <A>
     * @param collection the collection.
     * @param predicate the predicate.
     * @return the first matching item, or null if no match is found.
     */
    public static <A> A firstMatch( Collection<A> collection, Predicate<A> predicate )
    {
        return collection.stream()
            .filter( predicate )
            .findFirst()
            .orElse( null );
    }

    /**
     * Returns the intersection of the given collections.
     *
     * @param <A>
     * @param c1 the first collection.
     * @param c2 the second collection.
     * @return the intersection of the collections.
     */
    public static <A> Collection<A> intersection( Collection<A> c1, Collection<A> c2 )
    {
        Set<A> set1 = new HashSet<>( c1 );
        set1.retainAll( new HashSet<>( c2 ) );
        return set1;
    }

    /**
     * Returns all elements which are contained by {@code collection1} but not
     * contained by {@code collection2} as an immutable list.
     *
     * @param <T>
     * @param collection1 the first collection.
     * @param collection2 the second collection.
     * @return all elements in {@code collection1} not in {@code collection2}.
     */
    public static <A> List<A> difference( Collection<A> collection1, Collection<A> collection2 )
    {
        List<A> list = new ArrayList<>( collection1 );
        list.removeAll( collection2 );
        return Collections.unmodifiableList( list );
    }

    /**
     * Returns a list that contains all the members of one or more collections.
     * No check for duplicates is made.
     *
     * @param <T>
     * @param collections the collections to be concatenated.
     * @return the concatenated collections.
     */
    @SafeVarargs
    public static <T> List<T> concat( Collection<T>... collections )
    {
        return Arrays.stream( collections )
            .flatMap( Collection::stream )
            .collect( toList() );
    }

    /**
     * Searches for and returns the first string which starts with the given
     * prefix. Removes the match from the collection.
     *
     * @param collection the collection.
     * @param prefix the string prefix.
     * @return a string, or null if no matches.
     */
    public static String popStartsWith( Collection<String> collection, String prefix )
    {
        Iterator<String> iterator = collection.iterator();

        while ( iterator.hasNext() )
        {
            String element = iterator.next();

            if ( element != null && element.startsWith( prefix ) )
            {
                iterator.remove();
                return element;
            }
        }

        return null;
    }

    /**
     * Applies the given consumer to each item in the given collection after
     * filtering out null items.
     *
     * @param collection the collection.
     * @param consumer the consumer.
     */
    public static <E> void nullSafeForEach( Collection<E> collection, Consumer<E> consumer )
    {
        collection.stream()
            .filter( Objects::nonNull )
            .forEach( consumer );
    }

    /**
     * Returns an empty list if the given list is null, if not returns the list.
     *
     * @param list the list
     * @return a non-null list.
     */
    public static <T> List<T> emptyIfNull( List<T> list )
    {
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Returns an empty set if the given set is null, if not returns the set.
     *
     * @param set the set.
     * @return a non-null set.
     */
    public static <T> Set<T> emptyIfNull( Set<T> set )
    {
        return set != null ? set : new HashSet<>();
    }

    /**
     * Adds an item not already present in the target collection
     *
     * @param collection collection to add item to.
     * @param item item to add.
     */
    public static <E> void addUnique( Collection<E> collection, E item )
    {
        if ( !collection.contains( item ) )
        {
            collection.add( item );
        }
    }

    /**
     * Adds all items not already present in the target collection, preserving
     * the order of the items added (if the collection preserves them).
     *
     * @param collection collection to add items to.
     * @param items collection of items to add.
     */
    public static <E> void addAllUnique( Collection<E> collection, Collection<? extends E> items )
    {
        for ( E item : items )
        {
            addUnique( collection, item );
        }
    }

    /**
     * Indicates whether the given collection is null or empty.
     *
     * @param collection the collection, may be null.
     * @return true if the given collection is null or empty, false otherwise.
     */
    public static boolean isEmpty( Collection<?> collection )
    {
        return collection == null || collection.isEmpty();
    }

    /**
     * Adds the given object to the given collection if the object is not null.
     *
     * @param <T>
     * @param collection the collection.
     * @param object the object to add.
     * @return true if this collection changed as a result of the call.
     */
    public static <T> boolean addIfNotNull( Collection<T> collection, T object )
    {
        if ( object != null )
        {
            return collection.add( object );
        }

        return false;
    }

    /**
     * Returns a map of 1 or more key/value pairs.
     */
    public static <K, V> Map<K, V> mapOf( K key, V value, Object... keysAndValues )
    {
        List<Map.Entry<K, V>> entries = new ArrayList<>( 1 + keysAndValues.length / 2 );

        entries.add( Map.entry( key, value ) );

        for ( int i = 1; i < keysAndValues.length; i += 2 )
        {
            entries.add( Map.entry( (K) keysAndValues[i - 1], (V) keysAndValues[i] ) );
        }

        return Map.ofEntries( entries.toArray( new Map.Entry[entries.size()] ) );
    }

    /**
     * Convert an Iterator to a Stream.
     *
     * @param iterator The Iterator to convert to a stream.
     * @return A stream of the iterable.
     */
    public static <E> Stream<E> iterableToStream( Iterator<E> iterator )
    {
        return StreamSupport.stream( Spliterators.spliteratorUnknownSize( iterator,
            Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE ), false );
    }

    /**
     * Find duplicate item in given collection.
     *
     * @param collection the collection to be checked.
     * @param <T> The object type of the collection item.
     * @return Set of duplicate items.
     */
    public static <T> Set<T> findDuplicates( Collection<T> collection )
    {
        if ( CollectionUtils.isEmpty( collection ) )
        {
            return Set.of();
        }
        Set<T> duplicates = new HashSet<>();
        Set<T> uniques = new HashSet<>();

        for ( T t : collection )
        {
            if ( !uniques.add( t ) )
            {
                duplicates.add( t );
            }
        }

        return duplicates;
    }
}
