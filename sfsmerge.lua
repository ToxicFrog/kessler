-- usage: sfsmerge <target> <merge1> [merge2 ...]
-- format of an SFS file:
-- SFS := {ATOM}
-- ATOM := COMMENT | VALUE | STRUCTURE
-- COMMENT := // .*
-- VALUE := name '=' value
-- STRUCTURE := capsname '{' SFS '}'
--
-- All we care about from the merged files are the VESSEL and CREW blocks.
-- We ignore elapsed time - the time in the master file overrides everything.
-- We care about crew only so that we don't end up with multiple rockets using the same crew,
-- or rockets using crew that are already dead or don't exist yet.

require "util.init"

local sfs = require "sfs"

function log(...)
    io.stderr:write(table.concat(table.map({...}, tostring), "\t").."\n")
end

function main(...)
    local argv = { ... }
    
--    backup(argv[1])
    
    local original = sfs.load(argv[1])
    
    for i=2,#argv do
        original:merge(sfs.load(argv[i]))
    end
    
    print(original:write())
end

function backup(file)
    io.writefile(file.."."..os.date("%F %H:%M:%S")..".bak", io.readfile(file))
end

return main(...)
