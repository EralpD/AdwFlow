-- KEYS: new challenge, active challenge pointer, resend cooldown (same hash slot).
-- ARGV: challenge id, purpose, user id, auth version, digest, ttl ms, cooldown ms.
if redis.call('EXISTS', KEYS[3]) == 1 then return 0 end
if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
redis.call('HSET', KEYS[1], 'purpose', ARGV[2], 'uid', ARGV[3], 'version', ARGV[4],
    'digest', ARGV[5], 'attempts', '0')
redis.call('PEXPIRE', KEYS[1], ARGV[6])
redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[6])
redis.call('SET', KEYS[3], '1', 'PX', ARGV[7])
return 1
