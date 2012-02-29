-- permit 'fmt % foo' and 'fmt % { foo, bar }'
getmetatable("").__mod = function(fmt, args)
	if type(args) == "table" then
		return fmt:format(unpack(args))
	else
		return fmt:format(args)
	end
end

-- string trim(string) - remove whitespace from start and end
function string.trim(s)
        return s:gsub('^%s*', ''):gsub('%s*$', '')
end

-- string join(seperator, ...) - concatenate strings
function string.join(joiner, ...)
        return table.concat({...}, joiner)
end

string.tonumber = tonumber

-- string... split(string, pattern. max) - break up string on pattern
-- default value for pattern is to split on whitespace
-- default value for max is infinity
function string.split(s, pat, max)
        pat = pat or "%s+"
        max = max or nil
        local count = 0
        local i = 1
        local result = { 1 }
        
        local function splitter(sof, eof)
                result[#result] = s:sub(result[#result], sof-1)
                result[#result+1] = eof
        end
        
        if pat == "" then return s end

        s:gsub("()"..pat.."()", splitter, max)
        
        result[#result] = s:sub(result[#result], #s)

        return unpack(result)
end

-- rfind - as string.find() only backwards
function string.rfind (s, pattern, rinit, plain)
        -- if rinit is set, we basically trim the last rinit characters from the string
        s = s:sub(rinit, -1)
        
        local old_R = {}
        local R = { s:find(pattern, 1, plain) }

        while true do
                if R[1] == nil then return unpack(old_R) end
                old_R,R = R,{ s:find(pattern, R[2]+1) }
        end
end

-- count - count the number of occurences of a regex
function string.count(s, pattern)
        local count = 0
        for match in s:gmatch(pattern) do
                count = count+1
        end
        return count
end

