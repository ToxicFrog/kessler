-- invoke as: sfsupload host port name file

function main(...)
    require "socket"
    require "util.init"
    
    local host,port,name,file = ...
    
    local sock = assert(socket.connect(host, port:tonumber()))
    assert(sock:send(name.."\n"))
    assert(sock:send(assert(io.readfile(file))))
    assert(sock:send("\nEND\n"))
    
    local buf = assert(sock:receive("*a"))
    print(buf)
end

return main(...)
