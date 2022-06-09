import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
public class Project2{
	//var to read and store input file
	static BufferedReader readFile=null;
	//data for movies and seats
	static ArrayList<Project2> movieList;
	String movieName;
	int totSeats;
	boolean checkServer;
	//number of customers and box office agents
	static int totCutomers=50;
	static int totAgents=2;
	//semaphore array for customers
	static Project2[] done;
	//array of customer semaphores for agents to keep track
	Semaphore served;
	//mutual exclusion
	static Semaphore mutqueueAgents=new Semaphore(1);
	static Semaphore mutqueueTaker=new Semaphore(1);
	static Semaphore mutqueueWorker=new Semaphore(1);
	static Semaphore mutMovieArray=new Semaphore(1);
	//required for queue
	static Semaphore agentReady=new Semaphore(0);
	static Semaphore takerReady=new Semaphore(0);
	static Semaphore workerReady=new Semaphore(0);
	//queues
	static Queue<String> queueAgents=new LinkedList<String>();
	static Queue<Integer> queueTaker=new LinkedList<Integer>();
	static Queue<String> queueWorker=new LinkedList<String>();
	//for random numbers
	Random ran=new Random();
	//default constructor
	public Project2() {
	}
	//movies array List
	public Project2(String name,int seats) {
		movieName=name;
		totSeats=seats;
	}
	//customer semaphore
	public Project2(Semaphore s,Boolean c) {
		served=s;
		checkServer=c;
	}
	//customer thread
	class Customer extends Thread {
		//declare variables
		private String order;
		private int id;
		private int movieId;
		public Customer(int i) {
			id=i;
		}
		// process of a customer
		public void run() {
			try {
				chooseMovie(); //get movie
				mutqueueAgents.acquire();
				enqueueAgents(); //get agent
				agentReady.release(); //customer ready for agent
				mutqueueAgents.release();
				done[id].served.acquire();
				//check if movie available
				if(ticAvai()==true) {
					mutqueueTaker.acquire();
					enqueueTaker();  //get in line for ticket taker
					takerReady.release(); //call ticket taker
					mutqueueTaker.release();
					done[id].served.acquire(); //ticket taken
					//customer going to concession stand?
					if(concessionStand()==true) {
						chooseOrder();  //pick order
						mutqueueWorker.acquire();
						enqueueWorker();  // get the line for concession stand worker
						workerReady.release(); //call concession stand worker
						mutqueueWorker.release();
						done[id].served.acquire(); //get order
					}
					// enter theater
					enterThea();
				}
			} 
			catch (InterruptedException e) {
				System.out.println("Customer error id: "+id);
			}
		}
		//Add customer to box office agent queue
		private void enqueueAgents() {
			queueAgents.add(id+" "+movieId);
		}
		//Add customer to ticket taker queue with customer ID
		private void enqueueTaker() {
			queueTaker.add(id);
			System.out.println("Customer "+id+" in line to see ticket taker");
		}
		//Add customer to concession stand worker queue with order
		private void enqueueWorker() {
			queueWorker.add(id+"\t"+order);
			System.out.println("Customer "+id+" in line to buy "+order);
		}
		//movie ticket available
		private boolean ticAvai() {
			return done[id].checkServer;
		}
		//customer enters the movie
		private void enterThea() {
			System.out.println("Customer "+id+" enters theatre to see "+movieList.get(movieId).movieName);
		}
		//pick random movie
		private void chooseMovie() {
			movieId=ran.nextInt(movieList.size());
			System.out.println("Customer "+id+" created, buying ticket to "+movieList.get(movieId).movieName);
		}
		// pick random chance of going
		private boolean concessionStand() {
			int rand=ran.nextInt(2);
			if(rand==0)
				return true;
			else
				return false;
		}
		// pick random item
		private void chooseOrder() {
			int rand=ran.nextInt(3);
			if(rand==0)
				order="Popcorn";
			else if(rand==1)
				order="Soda";
			else if(rand==2)
				order="Popcorn and Soda";
		}
	}
	class BoxOfficeAgent extends Thread {
		//declare variables
		private String getCust;
		private int agentId;
		private int custMovie;
		private int custId;
		private boolean avai;

		public BoxOfficeAgent(int id) {
			agentId=id;
			System.out.println("Box Office Agent "+agentId+" created");
		}
		//run by the queue and check movie availabilty to process customer
		public void run() {
			while(true) {
				try {
					agentReady.acquire(); //get queue
					mutqueueAgents.acquire();
					dequeueAgents(); //attend customer
					mutqueueAgents.release();
					mutMovieArray.acquire();
					movieAvai(); //is movie available
					mutMovieArray.release();
					processAgent(); //process customer
					done[custId].served.release();
				}
				catch(InterruptedException e){
					System.out.println("BOA error customer "+agentId);
				}
			}
		}
		//process box office agent
		private void processAgent() {
			try {
				sleep(900);
			}
			catch (InterruptedException e)
			{}
			if(avai==true)
				System.out.println("Box office agent "+agentId+" sold ticket for "+movieList.get(custMovie).movieName+" to customer "+custId);
			done[custId].checkServer=avai;
		}
		//remove agent when done
		private void dequeueAgents() {
			getCust=queueAgents.remove();
			custId=Integer.parseInt(getCust.split(" ")[0]);
			custMovie=Integer.parseInt(getCust.split(" ")[1]);
			System.out.println("Box office agent "+agentId+" serving customer "+custId);
		}
		//if movie full or not
		private void movieAvai() {
			if(movieList.get(custMovie).totSeats>0) {
				movieList.get(custMovie).totSeats--;
				avai=true;
			}
			else
				avai=false;
		}
	}
	
	class TicketTaker extends Thread {
		//declare id variable
		private int custId;
		public TicketTaker() {
			System.out.println("Ticket Taker created");
		}
		//attend customer queue
		public void run() {
			while(true) {
				try {
					takerReady.acquire(); //get queue
					mutqueueTaker.acquire();
					dequeueTaker(); //attend customer
					mutqueueTaker.release();
					ProcessTt(); //process customer
					done[custId].served.release();
				}
				catch(InterruptedException e){
					System.out.println("TT error customer "+custId);
				}
			}
		}
		// ticker taker process
		private void ProcessTt() {
			try {
				sleep(150);
			}
			catch (InterruptedException e)
			{}
    		System.out.println("Ticket taken from customer "+custId);
		}
		private void dequeueTaker()
		{
			custId=queueTaker.remove();
		}
	}
	
	class ConcessionStandWorker extends Thread {
		//declare variables
		private String customerOrder;
		private String getCust;
		private int custId;
		public ConcessionStandWorker() {
			System.out.println("Concession Stand Worker created");
			System.out.println("Theatre is open");
		}
		// attend customer queue
		public void run() {
			while(true) {
				try {
					workerReady.acquire(); //get queue
					mutqueueWorker.acquire();
					dequeueWorker(); //attend customer
					mutqueueWorker.release();
					ProcessWorker(); //process customer
					done[custId].served.release();
				}
				catch(InterruptedException e){
					System.out.println("CSW error customer "+custId);
				}
			}
		}
		// concession order process
		private void ProcessWorker() {
			custId=Integer.parseInt(getCust.split("\t")[0]); //ID
			customerOrder=getCust.split("\t")[1];   //order
			System.out.println("Order for "+customerOrder+" taken from customer "+custId);
    		try {
				sleep(1800);
			}
    		catch (InterruptedException e)
			{}
    		System.out.println(customerOrder+" given to customer "+custId);
			System.out.println(custId+" receives "+ customerOrder);
		}
		// remove worker when done
		private void dequeueWorker() {
			getCust=queueWorker.remove();
		}
	}

	public static void main(String[] args) {
		//read input file to make movie and seats arraylist
		movieList=new ArrayList<Project2>();
		//open file
		File file = new File("movies.txt");
		try {
			readFile = new BufferedReader(new FileReader(file));
		}
		//if nothing found
		catch (FileNotFoundException e) {
			System.out.println("No file found");
			System.exit(1);
		}
		//add movies from file
		try {
			String line=null;
			while((line= readFile.readLine()) != null) {
				try {
					movieList.add(new Project2(line.split("\t")[0],Integer.parseInt(line.split("\t")[1])));
				}
				catch(NumberFormatException e) {
					System.out.println("incorrect format");
				}
			}
		} 
		catch (IOException e) {
			System.out.println("No file found");
			System.exit(1);
		}

		//create necessary BoxOfficeAgent threads
		Project2 theater=new Project2();
		for(int i=0;i<totAgents;i++) {
    		BoxOfficeAgent boa=theater.new BoxOfficeAgent(i);
    		boa.start();
    	}
		//create TicketTaker thread
		TicketTaker tt=theater.new TicketTaker();
		tt.start();
		//create ConcessionStandWorker thread
		ConcessionStandWorker csw=theater.new ConcessionStandWorker();
		csw.start();
		//threads for each customer
		Thread[] customer=new Thread[totCutomers];
		done=new Project2[totCutomers];
		//run customer threads
		for(int i=0;i<totCutomers;i++) {
			customer[i]=theater.new Customer(i);
			done[i]=new Project2(new Semaphore(0),false);
			customer[i].start();
		}
		//join customer threades
		for(int i=0;i<totCutomers;i++) {
    		try {
				customer[i].join();
				System.out.println("Joined customer "+i);
			}
			catch (InterruptedException e)
			{}
    	}
		//exit
    	System.exit(0);
	}
}
