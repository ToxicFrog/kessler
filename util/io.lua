function io.writefile(name, data)
    local fd = io.open(name, "wb")
    local r,e = fd:write(data)
    fd:close()
    return r,e
end

function io.readfile(name)
    local fd = io.open(name, "rb")
    local r,e = fd:read('*a')
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
