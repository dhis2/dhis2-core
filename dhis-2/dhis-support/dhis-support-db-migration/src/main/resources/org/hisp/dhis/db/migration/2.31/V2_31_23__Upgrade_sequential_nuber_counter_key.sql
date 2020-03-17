UPDATE sequentialnumbercounter SET key = substring(key, 12, (length(key)-12)) WHERE key LIKE 'SEQUENTIAL(%)';

DELETE
FROM sequentialnumbercounter
WHERE id not in (
    SELECT a.id
    from sequentialnumbercounter a
             inner join (
        select owneruid, key, max(counter) counter
        from sequentialnumbercounter
        group by owneruid, key
    ) b on a.key = b.key and a.owneruid = b.owneruid and a.counter = b.counter);