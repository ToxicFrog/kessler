list = table

-- recursively display the contents of a table
-- does not generate something the terp can read; use table.dump() for that
function table.print(T, prefix)
        assert(T, "bad argument to table.print")
        local done = {}
        local function tprint_r(T, prefix)
                for k,v in pairs(T) do
                        print(prefix..tostring(k),'=',tostring(v))
                        if type(v) == 'table' then
                                if not done[v] then
                                        done[v] = true
                                        tprint_r(v, prefix.."  ")
                                end
                        end
                end
        end
        done[T] = true
        tprint_r(T, prefix or "")
end

function table.copy(T)
    local R = {}
    for k,v in pairs(T) do R[k] = v end
    return R
end

function table.map(T, f)
    local R = {}
    
    for i,v in ipairs(T) do
        R[i] = f(v)
    end
    
    return R
end

function table.filter(T, f)
    local R = {}
    
    for i,v in ipairs(T) do
        if f(v) then
            R[#R] = v
        end
    end
    
    return R
end

-- an unpack that respects t.n rather than using #
local _unpack = unpack
function unpack(t, first, last)
	first = first or 1
	last  = last or t.n or #t
	return _unpack(t, first, last)
end
table.unpack = unpack

-- converse of unpack
function table.pack(...)
    return { n = select('#', ...), ... }
end

function table.isomorphic(t, r)
    for k,v in pairs(t) do
        if type(v) ~= type(r[k]) then return false
        elseif type(v) == "table" and not table.isomorphic(v, r[k]) then return false
        end
    end
    for k,v in pairs(r) do
        if type(v) ~= type(t[k]) then return false
        elseif type(v) == "table" and not table.isomorphic(v, t[k]) then return false
        end
    end
    
    return true
end
