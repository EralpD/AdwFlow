-- KEYS: challenge, active pointer. ARGV: challenge id, purpose, digest, max attempts.
if redis.call('HGET', KEYS[1], 'purpose') ~= ARGV[2] then return 0 end
if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0')
if attempts >= tonumber(ARGV[4]) then return 0 end
local stored = redis.call('HGET', KEYS[1], 'digest')
-- Compare every byte of the fixed-length digest, without a prefix-dependent early return.
local difference = 0
if not stored or string.len(stored) ~= string.len(ARGV[3]) then
    difference = 1
else
    for i = 1, string.len(stored) do
        difference = bit.bor(difference, bit.bxor(string.byte(stored, i), string.byte(ARGV[3], i)))
    end
end
if difference == 0 then
    redis.call('DEL', KEYS[1], KEYS[2])
    return 1
end
attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
if attempts >= tonumber(ARGV[4]) then redis.call('DEL', KEYS[1], KEYS[2]) end
return 0
