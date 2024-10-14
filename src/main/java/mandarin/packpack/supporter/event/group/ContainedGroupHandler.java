package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ContainedGroupHandler implements GroupHandler {
    public final String name;
    private final boolean raw;

    private final List<List<Integer>> containedGroup;
    private final List<EventGroup> groups = new ArrayList<>();

    public ContainedGroupHandler(@Nonnull List<List<Integer>> containedGroup, @Nonnull String name, boolean raw) {
        this.containedGroup = containedGroup;
        this.name = name;
        this.raw = raw;
    }

    @Override
    public boolean handleEvent(Schedule schedule) {
        if(schedule instanceof StageSchedule) {
            if(!raw && ((StageSchedule) schedule).stages.isEmpty())
                return false;
            else if(raw && ((StageSchedule) schedule).unknownStages.isEmpty())
                return false;

            if(containsOnlyValidStages((StageSchedule) schedule)) {
                boolean added = false;

                for(EventGroup group : groups) {
                    if(groupCanBeApplied(group.schedules, (StageSchedule) schedule)) {
                        added |= group.addStage((StageSchedule) schedule, raw);
                    }
                }

                if(!added && isPrimarySchedule((StageSchedule) schedule)) {
                    EventGroup newGroup = new ContainedEventGroup(containedGroup, name);

                    boolean result = newGroup.addStage((StageSchedule) schedule, raw);

                    groups.add(newGroup);

                    return result;
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void clear() {
        groups.clear();
    }

    @Override
    public List<EventGroup> getGroups() {
        return groups;
    }

    private boolean containsOnlyValidStages(StageSchedule schedule) {
        for (List<Integer> group : containedGroup) {
            ArrayList<Integer> data = new ArrayList<>();

            if(raw) {
                for (String unknown : schedule.unknownStages) {
                    if(!StaticStore.isNumeric(unknown))
                        return false;

                    int id = StaticStore.safeParseInt(unknown);

                    data.add(id);
                }
            } else {
                for (StageMap stm : schedule.stages) {
                    int id = EventFactor.stageToInteger(stm);

                    if (id == -1) {
                        return false;
                    } else {
                        data.add(id);
                    }
                }
            }

            if(data.containsAll(group))
                return true;
        }

        return false;
    }

    private boolean isPrimarySchedule(StageSchedule schedule) {
        List<Integer> primary = containedGroup.getFirst();

        List<Integer> ids = new ArrayList<>();

        if(raw) {
            for(int i = 0; i < schedule.unknownStages.size(); i++) {
                String unknown = schedule.unknownStages.get(i);

                if(!StaticStore.isNumeric(unknown))
                    return false;

                ids.add(StaticStore.safeParseInt(unknown));
            }
        } else {
            for(int i = 0; i < schedule.stages.size(); i++) {
                StageMap map = schedule.stages.get(i);

                int id = EventFactor.stageToInteger(map);

                if(id == -1)
                    return false;
                else
                    ids.add(id);
            }
        }

        return new HashSet<>(primary).containsAll(ids);
    }

    private boolean groupCanBeApplied(Schedule[] schedules, StageSchedule schedule) {
        if(schedules[0] == null && isPrimarySchedule(schedule))
            return true;
        else if(schedules[0] != null && schedules[0] instanceof StageSchedule) {
            return ((StageSchedule) schedules[0]).date.dateStart.compare(schedule.date.dateStart) <= 0 &&
                    ((StageSchedule) schedules[0]).date.dateEnd.compare(schedule.date.dateEnd) >= 0;
        }

        return false;
    }

    @Override
    public String toString() {
        return "ContainedGroupHandler{" +
                "name='" + name + '\'' +
                ", raw=" + raw +
                ", containedGroup=" + containedGroup +
                ", groups=" + groups +
                '}';
    }
}
