package mandarin.packpack.supporter.event.group;

import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.Schedule;
import mandarin.packpack.supporter.event.StageSchedule;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class NormalGroupHandler implements GroupHandler {

    public final String name;
    public final boolean raw;

    private final List<List<Integer>> groupData;
    private final ArrayList<EventGroup> groups = new ArrayList<>();

    public NormalGroupHandler(@Nonnull List<List<Integer>> groupData, @Nonnull String name, boolean raw) {
        this.groupData = groupData;
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

                for (EventGroup group : groups) {
                    if(group.finished)
                        continue;

                    //Existing group
                    if(group.getDate() != null) {
                        if (((StageSchedule) schedule).date.equals(group.getDate())) {
                            added |= group.addStage((StageSchedule) schedule, raw);
                        }
                    }
                }

                if(!added) {
                    //No group where can put this schedule found

                    EventGroup newGroup = new NormalEventGroup(groupData, name);

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
        for (List<Integer> group : groupData) {
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
}
