function io.writefile(name, data)
    local fd = assert(io.open(name, "w"))
    local r,e = assert(fd:write(data))
    fd:close()
    return r,e
end

function io.readfile(name)
    local fd = assert(io.open(name, "r"))
    local r,e = assert(fd:read('*a'))
    fd:close()
    return r,e
end

function io.exists(name)
    local fd,err = io.open(name, "r")
    if not fd then
        return false,err
    end
    fd:close()
    return true
end
