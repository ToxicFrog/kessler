-- invoke as: sfsupload host port name file

function main(...)
    require "socket"
    require "util.init"
    
    local host,port,name,file,output = ...
    
    local sock = assert(socket.connect(host, port:tonumber()))
    assert(sock:send(name.."\n"))
    assert(sock:send(assert(io.readfile(file))))
    assert(sock:send("\nEND\n"))
    
    local buf = assert(sock:receive("*a"))
    if output then
        io.output(io.open(output, "wb"))
    end
    io.write(buf)
end

return main(...)
