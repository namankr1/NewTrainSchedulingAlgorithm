import java.io.File;
import java.io.PrintStream;
import java.util.*;

public class ScheduleByDivision {

    private List<String> stationIdList;
    private List<String> stationNameList;
    private List<Double> stationDistanceList;
    private List<Boolean> stationIsDirectLineList;
    private List<Integer> stationNoOFUpPlatformList;
    private List<Integer> stationNoOFDownPlatformList;
    private List<Integer> stationNoOFDualPlatformList;
    private List<Integer> stationNoOfUpTrackList;
    private List<Integer> stationNoOfDownTrackList;
    private List<Integer> stationNoOfDualTrackList;
    private List<Integer> stopTimeList;
    private Queue<Path> bestAns;

    public List<Path> getSmallPart(String pathTemp, int firstIndex, int lastIndex, int noOfPaths, TrainTime sourceTime,
                                   int minDelayBwTrains, String pathRouteSpeedFile, String newTrainType, String pathOldTrainSchedule, boolean isSingleDay, int trainDay, double ratio,
                                   boolean onSourceTime){

        return new KBestSchedule().getScheduleNewTrain(pathTemp, this.stationIdList.subList(firstIndex,lastIndex),
                this.stationNameList.subList(firstIndex,lastIndex), this.stationDistanceList.subList(firstIndex,lastIndex),
                this.stationIsDirectLineList.subList(firstIndex,lastIndex), this.stationNoOFUpPlatformList.subList(firstIndex,lastIndex),
                this.stationNoOFDownPlatformList.subList(firstIndex,lastIndex), this.stationNoOFDualPlatformList.subList(firstIndex,lastIndex),
                this.stationNoOfUpTrackList.subList(firstIndex,lastIndex), this.stationNoOfDownTrackList.subList(firstIndex,lastIndex),
                this.stationNoOfDualTrackList.subList(firstIndex,lastIndex), noOfPaths, sourceTime, minDelayBwTrains, pathRouteSpeedFile,newTrainType,
                this.stopTimeList.subList(firstIndex,lastIndex), pathOldTrainSchedule, trainDay, isSingleDay, false, ratio, onSourceTime);
    }

    public void getPathsRecur(String pathTemp, int i, int stationGroupSizeForPart, int noOfPaths, TrainTime sourceTime,
                              int minDelayBwTrains, String pathRouteSpeedFile, String newTrainType, String pathOldTrainSchedule,
                              boolean isSingleDay, int trainDay, double ratio, Path pathPrevious){
        if(i!=0 && pathPrevious==null){
            System.out.println("Previous path cant be null");
            return;
        }
        if(i>=this.stationIdList.size()){
            return;
        }
        int last;
        if((i+stationGroupSizeForPart+1)<this.stationIdList.size()){
            last = i+stationGroupSizeForPart+1;
        }
        else{
            last = this.stationIdList.size();
        }

        // add 0 to wait time of first
        noOfPaths = (i==0)?noOfPaths*2:1;
        List<Path> paths = getSmallPart(pathTemp, i, last, noOfPaths, sourceTime, minDelayBwTrains,
                pathRouteSpeedFile,newTrainType, pathOldTrainSchedule, isSingleDay, trainDay, ratio, (i!=0));

        for(Path path: paths){
            // System.out.println(" i = " + i + " last = " + last );
            List<Node> nodes1;
            List<Double> weights1;
            TrainTime sourceTime1;
            try {
                sourceTime1 = path.getNodeList().get(path.getNodeList().size() - 2).getTime();
            }
            catch (Exception e){
                sourceTime1 = null;
            }
            int i1 = i+stationGroupSizeForPart;

            nodes1 = path.getNodeList();
            weights1 = path.getWeightList();
            Path tempPath;
            if(i==0){
                tempPath = path;
            }
            else {
                tempPath = pathPrevious;
                try {
                    tempPath = tempPath.removeLastNode();
                    double tempPathCost = tempPath.pathCost();
                    tempPath = tempPath.removeLastNode();
                    for (int i2 = 1; i2 < nodes1.size(); i2++) {
                        tempPath = tempPath.append(nodes1.get(i2), tempPathCost + weights1.get(i2));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
            if(last<this.stationIdList.size()){
                getPathsRecur(pathTemp, i1, stationGroupSizeForPart,noOfPaths, sourceTime1, minDelayBwTrains,
                        pathRouteSpeedFile,newTrainType,pathOldTrainSchedule,
                        isSingleDay, trainDay, ratio, tempPath);
            }
            else{
                this.bestAns.add(tempPath);
                System.out.println("Accepted Path Found : " + tempPath.toString() + " cost: " + tempPath.pathCost());
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public void scheduleByBreaking(String pathTemp, String pathRoute, String pathBestRoute,
                                   String pathOldTrainSchedule,
                                   boolean isSingleDay, int trainDay, double ratio, String pathLog, String pathRouteSpeedFile, String newTrainType,
                                   TrainTime sourceTime){
        if(sourceTime!=null){
            sourceTime = new TrainTime(sourceTime);
        }
        if(ratio<1){
            System.out.println("Ratio must be greater than 1.0");
            return;
        }
        Scheduler scheduler = new Scheduler();
        if(!scheduler.addRouteFromFile(pathRoute)){
            System.out.println("Unable to load route file");
            return;
        }
        ArrayList<Integer> stopTime = new ArrayList<>();
        for(int i=0;i<scheduler.getStationIdList().size();i++) {
            stopTime.add(0);
        }
        // stopTime.set(5, 2.0);
        // stopTime.set(12, 4.0);
        // stopTime.set(22, 4.0);

        this.stationIdList = scheduler.getStationIdList();
        this.stationNameList = scheduler.getStationNameList();
        this.stationDistanceList = scheduler.getStationDistanceList();
        this.stationIsDirectLineList = scheduler.getStationDirectLineList();
        this.stationNoOFUpPlatformList = scheduler.getStationNoOfUpPlatformList();
        this.stationNoOFDownPlatformList = scheduler.getStationNoOfDownPlatformList();
        this.stationNoOFDualPlatformList = scheduler.getStationNoOfDualPlatformList();
        this.stationNoOfUpTrackList = scheduler.getStationNoOfUpTrackList();
        this.stationNoOfDownTrackList = scheduler.getStationNoOfDownTrackList();
        this.stationNoOfDualTrackList = scheduler.getStationNoOfDualTrackList();
        this.stopTimeList = stopTime;

        int minDelayBwTrains = 3;
        int noOfPaths = 10;
        int stationGroupSizeForPart = (stopTime.size()/2);
        if(stationGroupSizeForPart>12){
            stationGroupSizeForPart=12;
        }
        double sumStopTimes =0;
        for(int i=0;i<stationDistanceList.size();i++){
            sumStopTimes +=stopTime.get(i);
        }

        String pathBestRouteFile = pathBestRoute + File.separator +"Type Break Day "+trainDay+" TrainType "+newTrainType +
                " maxRatio "+ratio +((sourceTime==null)?" unconditional ":" conditional ");
        this.bestAns = new PriorityQueue<>(Comparator.comparingDouble(Path::pathCost));

        try {
            PrintStream o1 = new PrintStream(new File(pathLog + File.separator + "Output Type Break Day "+trainDay+" TrainType "+newTrainType +
                    " maxRatio "+ratio +((sourceTime==null)?" unconditional.log":" conditional.log")));
            PrintStream console = System.out;
            System.setOut(o1);

            getPathsRecur(pathTemp,0,stationGroupSizeForPart, noOfPaths,sourceTime, minDelayBwTrains,pathRouteSpeedFile,newTrainType,
                    pathOldTrainSchedule, isSingleDay, trainDay, ratio,
                    null);

            int count=0;
            System.out.println();

            while(!this.bestAns.isEmpty() && count<noOfPaths){
                Path path = this.bestAns.remove();
                System.out.println("Path Found : " + path.toString() + " cost: " + path.pathCost());
                scheduler.writePathsToFile(path,++count, pathBestRouteFile, stopTime, pathRouteSpeedFile,newTrainType,
                        scheduler.getStationNameList(), scheduler.getStationDistanceList());
            }
            System.setOut(console);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
