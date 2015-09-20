package ui;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.Date;
import java.util.zip.*;

import javax.swing.*;

//http://www2.census.gov/geo/docs/maps-data/data/baf/BlockAssign_ST06_CA.zip block assignment file
//http://www.census.gov/geo/maps-data/data/baf_description.html description
//http://www.census.gov/geo/maps-data/data/gazetteer2010.html try using these to get population data

//http://www2.census.gov/geo/docs/maps-data/data/gazetteer/census_tracts_list_01.txt
public class Download {
	public static boolean census_merge_working = true;
	public static boolean census_merge_old = true;
	
	public static int istate = -1;
	public static int cyear = -1;
	public static int vyear = -1;
	
	public static File vtd_file = null;
	public static File census_pop_file = null;
	public static File census_centroid_file = null;
	public static File census_tract_file = null;
	
	public static void main(String[] args) {
		downloadState(1,2010,2012,null,null);
	}
	public static void downloadState(int state, int census_year, int election_year, JDialog dlg, JLabel lbl) {
		downloadState(state, census_year, election_year, null, null,lbl); 
	}
	public static boolean downloadData(JDialog dlg, JLabel lbl) {
		istate = -1;
		String state = (String)JOptionPane.showInputDialog(MainFrame.mainframe, "Select the state", "Select state.", 0, null, states, states[0]);
		if( state == null)
			return false;
		for( int i = 0; i < states.length; i++) {
			if( state.equals(states[i])) {
				istate = i;
				break;
			}
		}
		if( istate <= 0) {
			return false;
		}
		int y = new Date().getYear()+1900;
		int y10 = y - y % 10;
		int y4 = y - y % 4;
		String[] cyears = new String[]{""+y10,""+(y10-10)};
		String[] eyears = new String[]{""+y4,""+(y4-4),""+(y4-8),""+(y4-12),""+(y4-16),""+(y4-20)};

		String scyear = (String)JOptionPane.showInputDialog(MainFrame.mainframe, "Select the census year.", "Select year.", 0, null, cyears, cyears[0]);
		if( scyear == null)
			return false;
		cyear =  Integer.parseInt(scyear);
		
		String svyear = (String)JOptionPane.showInputDialog(MainFrame.mainframe, "Select the election year for voting tabulation districts.", "Select election year.", 0, null, eyears, eyears[0]);
		if( svyear == null)
			return false;
		vyear = Integer.parseInt(svyear);
		
		JOptionPane.showMessageDialog(MainFrame.mainframe, "It may take a few minutes to download and extact the data.\n(hit okay)");

		if( !downloadState( istate,cyear,vyear,null, dlg,lbl)) {
			return false;
		}
		return true;
	}
	public static boolean downloadState(int state, int census_year, int election_year, String start_path, JDialog dlg, JLabel lbl) {
		if( start_path == null) {
			File f = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory();
			start_path = f.getAbsolutePath();
			if( !start_path.substring(start_path.length()-1).equals(File.separator)) {
				start_path += File.separator;
			}
			start_path += "autoredistrict_data"+File.separator;
		}
		if( !start_path.substring(start_path.length()-1).equals(File.separator)) {
			start_path += File.separator;
		}

		String path = start_path+states[state]+File.separator+census_year+File.separator;
		File f = new File(path);
		if( !f.exists()) { f.mkdirs(); }
		
		String census_tract_path = path;
		String census_centroid_path = path+"block_centroids"+File.separator;
		String census_pop_path = path+"block_pop"+File.separator;
		String census_vtd_path = path+election_year+File.separator+"vtd"+File.separator;
		
		File ftest1 = new File(census_vtd_path+"vtds.zip");
		File ftest2 = new File(census_pop_path+"block_pops.zip");
		File ftest3 = new File(census_centroid_path+"block_centroids.zip");
		File ftest4 = new File(census_tract_path+census_tract_filename(state,cyear));
		
		boolean download_census = true;
		boolean download_vtd = true;
		
		if( ftest1.exists()) {
			download_vtd = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "VTD shapefiles already exist.  Re-download?");
		}
		if( ftest4.exists()) {//ftest2.exists() && ftest3.exists()) {
			download_census = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Census files already exist.  Re-download?");
		}

		if( dlg != null) { dlg.show(); }
		try {
			if( download_vtd) {
				if( lbl != null) { lbl.setText("Downloading vtd shapfile..."); }
				download(census_vtd_url(state,census_year,election_year),census_vtd_path,"vtds.zip");
			}
			if( download_census && census_merge_working) {
				if( !census_merge_old) {
					if( lbl != null) { lbl.setText("Downloading census population..."); }
					download(census_tract_url(state,census_year),census_tract_path,census_tract_filename(state,cyear));
				} else {
					if( lbl != null) { lbl.setText("Downloading census population..."); }
					download(census_pop_url(state,census_year),census_pop_path,"block_pops.zip");
					if( lbl != null) { lbl.setText("Downloading census block centroids..."); }
					download(census_centroid_url(state,census_year),census_centroid_path,"block_centroids.zip");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			if( dlg != null) { dlg.hide(); }
			return false;
		}
		System.out.println("done downloading. extracting...");
		try {
			if( lbl != null) { lbl.setText("Extracting vtd shapfile..."); }
			unzip(census_vtd_path+"vtds.zip", census_vtd_path);
			if( census_merge_working && census_merge_old) {
				if( lbl != null) { lbl.setText("Extracting census population..."); }
				unzip(census_pop_path+"block_pops.zip", census_pop_path);
				if( lbl != null) { lbl.setText("Extracting census block centroids..."); }
				unzip(census_centroid_path+"block_centroids.zip", census_centroid_path);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if( dlg != null) { dlg.hide(); }
			return false;
		}
		census_centroid_file = new File(census_centroid_path+census_centroid_filename(state,census_year));
		census_pop_file = new File(census_pop_path+census_pop_filename(state,census_year));
		vtd_file = new File(census_vtd_path+census_vtd_filename(state,census_year,election_year));
		census_tract_file = new File(census_tract_path+census_tract_filename(state,census_year));

		if( dlg != null) { dlg.hide(); }
		System.out.println("done extracting.");
		return true;
	}
	public static String census_tract_url(int state, int year) {
		return "http://www2.census.gov/geo/docs/maps-data/data/gazetteer/"
			+"census_tracts_list_"+num(state)+".txt";
	}
	public static String census_centroid_url(int state, int year) {
		return "ftp://ftp2.census.gov/geo/pvs/tiger"+year+"st/"
				+num(state)+"_"+states[state]+"/"+num(state)+"/"
				+"tl_"+year+"_"+num(state)+"_tabblock"+shortyear(year)+".zip";
	}
	public static String census_pop_url(int state, int year) {
		return "ftp://ftp2.census.gov/geo/tiger/TIGER"+year+"BLKPOPHU/"
				+"tabblock"+year+"_"+num(state)+"_pophu.zip";

	}
	public static String census_vtd_url(int state, int year, int elec_year) {
		return "http://www2.census.gov/geo/tiger/TIGER"+elec_year+"/VTD/"
				+"tl_"+elec_year+"_"+num(state)+"_vtd"+shortyear(year)+".zip";
	}
	public static String census_tract_filename(int state, int year) {
		return ""
			+"census_tracts_list_"+num(state)+".txt";
	}

	
	public static String census_centroid_filename(int state, int year) {
		return ""
				+"tl_"+year+"_"+num(state)+"_tabblock"+shortyear(year)+".dbf";
	}
	public static String census_pop_filename(int state, int year) {
		return ""
				+"tabblock"+year+"_"+num(state)+"_pophu.dbf";

	}
	public static String census_vtd_filename(int state, int year, int elec_year) {
		return ""
				+"tl_"+elec_year+"_"+num(state)+"_vtd"+shortyear(year)+".shp";
	}
	public static String shortyear(int year) {
		String s = ""+year;
		return s.substring(2);
	}
	public static String num(int i) {
		String s = ""+i;
		if( s.length() < 2) { s = "0"+s; }
		return s;
	}
	
	public static String[] states = new String[]{
			"",
			"Alabama",
			"Alaska",
			"American Samoa",
			"Arizona",
			"Arkansas",
			"California",
			"Colorado",
			"Connecticut",
			"Delaware",
			"District of Columbia",
			"Florida",
			"Georgia",
			"Guam",
			"Hawaii",
			"Idaho",
			"Illinois",
			"Indiana",
			"Iowa",
			"Kansas",
			"Kentucky",
			"Louisiana",
			"Maine",
			"Maryland",
			"Massachusetts",
			"Michigan",
			"Minnesota",
			"Mississippi",
			"Missouri",
			"Montana",
			"Nebraska",
			"Nevada",
			"New Hampshire",
			"New Jersey",
			"New Mexico",
			"New York",
			"North Carolina",
			"North Dakota",
			"Ohio",
			"Oklahoma",
			"Oregon",
			"Pennsylvania",
			"Rhode Island",
			"South Carolina",
			"South Dakota",
			"Tennessee",
			"Texas",
			"Utah",
			"Vermont",
			"Virginia",
			"Washington",
			"West Virginia",
			"Wisconsin",
			"Wyoming",
			"American Samoa",
			"Guam",
			"Commonwealth Of The Northern Marianas Islands",
			"Puerto Rico",
			"Virgin Islands Of The United States",
			};
	
	public static boolean download(String url, String dest_path, String dest_name) throws Exception {
		System.out.println("downloading:");
		System.out.println("url :"+url);
		System.out.println("path:"+dest_path);
		System.out.println("file:"+dest_name);

		File f = new File(dest_path);
		if( !f.exists()) { f.mkdirs(); }

		URL website = new URL(url);
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		FileOutputStream fos = new FileOutputStream(dest_path+dest_name);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

		return true;
	}
	
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
    	System.out.println("unzipping "+zipFilePath);
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

}