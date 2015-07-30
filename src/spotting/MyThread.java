package spotting;

class MyThread extends Thread 
{
	
	public String trainDir;
	public String groundfilename;
	public String csvFilename;
	public int maxNodes;
	public int csize;
	public int consolidationSize;
	
    MyThread(String trainDir1, String groundfilename1, String csvFilename1, int maxNodes1, int csize1, int consolidationSize1) 
    {
        trainDir=trainDir1;
        groundfilename=groundfilename1;
        csvFilename=csvFilename1;
        maxNodes=maxNodes1;
        csize=csize1;
        consolidationSize=consolidationSize1;
    }
    
    
    public void run ( )
    {
    	System.out.println("m running with "+trainDir);
    	LucenePotentialEntities.spotter(trainDir, groundfilename, csvFilename, maxNodes, csize, consolidationSize);
    }
 

}

