-- inetd daemon for sfsmerge
-- upload an SFS, get back a merged SFS

-- The protocol it expects is:
-- one line with the name
-- the contents of the SFS
-- the text END

-- Upon receiving this, it needs to:
-- * create a merged file from all of the saved SFSs, most recent *first* (so that it contains the most recent version of every flight)
-- * send that merged version to the user (by dumping it on stdout)
-- * save the SFS, with name and timestamp, for future users

-- global configuration
SAVEDIR="/var/lib/kspmerge"

-- load libs
require "util.init"
local sfs = require "sfs"
local lfs = require "lfs"

log = function(...)
    local log = io.open("/var/log/kspmerge", "a")
    log:write(table.concat(table.map({...}, tostring), "\t").."\n")
    log:close()
end

function doMerge(buffer, name)
    local done = { [name] = true }
    
    -- merge in other SFSs
    log("Merging SFS")
    local merged = sfs.parse(buffer)
        
    log("Scanning save directory...")
    local saves = {}
    for file in lfs.dir(SAVEDIR) do
        if not file:match("^%.") then
            log("", file)
            table.insert(saves, file)
        end
    end
    table.sort(saves)
    
    for i=#saves,1,-1 do
        local file = saves[i]
        local mergename = file:match("%d+ (.*)")
        if not done[mergename] then
            log("Merging:", file, sfs.load(SAVEDIR.."/"..file))
            merged:merge(sfs.load(SAVEDIR.."/"..file), mergename)
            done[mergename] = true
        end
    end

    -- send to user
    io.write(merged:write())
end

function doSave(buffer, name, time)
    log("Saving SFS:", SAVEDIR.."/"..time.." "..name)
    assert(io.writefile(SAVEDIR.."/"..time.." "..name, buffer))
end

function main(...)
    -- read SFS from user
    local time = os.time()
    local name = io.read("*l")
    local buffer = {}
    
    local flags = {}
    name = name:gsub("%-%-(%w+)", function(flag) flags[flag] = true; return "" end)
    name = name:gsub("[^%w%s_]", "_")
    
    for line in io.lines() do
        if line == "END" then
            break
        end
        table.insert(buffer, line)
    end
    
    buffer = table.concat(buffer, "\n"):trim().."\n"
    log("Received save file from "..name)
    
    if not flags.nomerge then
        doMerge(buffer, name)
    else
        print("//")
    end
    
    -- save SFS
    if not flags.nosave then
        doSave(buffer, name, time)
    end
end

(function(success, message)
    if not success then
        local log = io.open("/var/log/kspmerge", "a")
        log:write(tostring(message).."\n")
        log:close()
    end
end)(pcall(main, ...))
