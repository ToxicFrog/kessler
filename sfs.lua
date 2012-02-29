local sfs = {}
local sfs_mt = {
    __index = sfs;
    __type = "SFS";
    __tostring = function(self) return "SFS ("..#self.crew..":"..#self.vessels..")" end;
}

-- come up with a comparable reduction of a CREW block. This is done by discarding all information that might vary with
-- game, such as time of death - keeping only name, brave/dumb values, and badS flag
local function hashCrew(crew)
    local keep = {
        name = true;
        brave = true;
        dumb = true;
        badS = true;
    }
    return (crew:gsub("\n%s+(%w+) = ([^\n]*)", function(key, value)
        if keep[key] then
            return nil
        else
            return ""
        end
    end))
end

local function hashOrbit(orbit)
    local keep = {
        SMA = true; -- semi-major axis
        ECC = true; -- eccentricity
        INC = true; -- inclination
        LPE = true; -- longitude of periapsis
        LAN = true; -- longitude of ascending node
    }
    return (orbit:gsub("\n%s+(%w+) = ([^\n]*)", function(key, value)
        if keep[key] then
            return nil
        else
            return ""
        end
    end))
end

-- come up with a comparable reduction of a VESSEL block. This is slightly trickier than CREW, since it works by
-- extracting the ship's crew roster and hashing it.
-- there are two special cases here: if the vessel has no crew (ie, is debris), or if the vessel has crewmembers
-- (0,1,2), indicating that it was the first ship created (and thus has Bill, Jeb, and Bob as crew)
-- in those cases, we instead generate a hash based on its time-invariant orbital characteristics
local function hashVessel(vessel, sfs)
    local roster = {}
    vessel:gsub("crew = (%d+)", function(id)
        -- the SFS file uses 0-indexed crew arrays; we need to compensate for that
        assert(sfs.crew[id:tonumber()+1], "crewmember "..id.." not found in "..tostring(sfs))
        table.insert(roster, hashCrew(sfs.crew[id:tonumber() +1]))
    end)
    
    -- no crew or Jeb on board- vessel is debris
    local Jeb = [[name = Jebediah Kerman%s+brave = 0.5%s+dumb = 0.5%s+badS = True]]
    
    if #roster == 0 or roster[1]:match(Jeb) then
        return hashOrbit(vessel:match("ORBIT %b{}"))
    end
    
    return table.concat(roster)
end

function sfs.load(name)
    return sfs.parse(io.readfile(name))
end

function sfs.parse(buf)
    local self = setmetatable({ crew = {}, vessels = {} }, sfs_mt)
    
    local function addCrew(crew)
        self:addCrew(crew)
        return ""
    end
    
    local function addVessel(vessel)
        self:addVessel(vessel)
        return ""
    end
    
    self.header = buf:gsub("CREW %b{}", addCrew)
            :gsub("VESSEL %b{}", addVessel)
            :trim()
            .."\n"
            
    return self
end

function sfs:save(name)
    return io.writefile(name, self:write())
end

function sfs:write()
    return table.concat({
        self.header,
        table.concat(self.crew, "\n"),
        table.concat(self.vessels, "\n"),
        "\n"
    }, "\n")
end

function sfs:addVessel(vessel)
    table.insert(self.vessels, vessel)
    self.vessels[hashVessel(vessel, self)] = #self.vessels
    return #self.vessels
end

function sfs:addCrew(crew)
    table.insert(self.crew, crew)
    self.crew[hashCrew(crew)] = #self.crew
    return #self.crew
end

-- merge the contents of two SFS files, skipping any crewmembers or vessels that
-- already exist in the destination.
-- To check if a given crewmember exists, we compare them (with the TOD marker
-- removed) to the crewmembers in the original - we remove the TOD marker
-- because they might be dead in one and alive in the other still.
-- To check if a given ship exists in both, we see if there's a ship with the
-- same crewmember stats - since the odds of two different ships having three
-- crewmembers with the same stats and names are infinitesmal.
function sfs:merge(src, name)
    local last_crew = #self.crew
    
    local function mergeCrew(crew)
        return self:addCrew(crew)
    end
    
    local function remapCrew(id)
        local crew = assert(src.crew[id:tonumber() +1], "No crewmember with id "..id.." in source SFS")
        return "crew = "..(mergeCrew(crew) - 1)
    end
    
    local function mergeVessel(vessel)
        vessel = vessel:gsub("crew = (%d+)", remapCrew)
        
        if name then
            vessel = vessel:gsub("name = ([^\n]+)", "name = %1 ("..name..")", 1)
        end
        
        return self:addVessel(vessel)
    end

    for _,vessel in ipairs(src.vessels) do
        local name = vessel:match("name = ([^\n]+)")
        if not self:containsVessel(vessel, src) then
            log("  Merging vessel '"..name.."'")
            -- mergeVessel will automatically merge in the crew records if needed
            mergeVessel(vessel)
        else
            log("  Skipping duplicate vessel '"..name.."'")
        end
    end
end

function sfs:containsVessel(vessel, src)
    return self.vessels[hashVessel(vessel, src or self)]
end

function sfs:containsCrew(crew)
    return self.crew[hashCrew(crew)]
end

return sfs
