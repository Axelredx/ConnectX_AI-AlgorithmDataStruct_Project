/* 
 * ConnectX is a generalised version of Connect4, in this project I've developped "AxelBrain"
 * an AI that can sees 6/8 moves ahead to compete against L0 and L1 AI.
 *
 * "AxelBrain", a project of Alex Rossi, mat:0001089916, alex.rossi7@studio.unibo.it;
 *  CST course a.y. 22/23, Alma Mater Studiorum, University of Bologna, Italy.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

**How to use the program (compile and setup):**
- Put 'AxelBrain' directory in 'connectx' directory;
- Command-line compile.  In the connectx/ directory run:

		javac -cp ".." *.java */*.java


**CXGame application:**

- Human vs Computer.  In the connectx/ directory run:
	
		java -cp ".." connectx.CXGame 6 7 4 connectx.L0.L0


- Computer vs Computer. In the connectx/ directory run:

		java -cp ".." connectx.CXGame 6 7 4 connectx.L0.L0 connectx.L1.L1


**CXPlayerTester application:**

- Output score only:

	java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1
	
	- Using AxelBrain AI:
		java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.AxelBrain.AxelBrain connectx.L1.L1 -t 1 -r 10

		java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L1.L1 connectx.AxelBrain.AxelBrain -t 1 -r 10


- Verbose output

	java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v


- Verbose output with customized timeout (1 sec) and number of game repetitions (10 rounds)

	java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v -t 1 -r 10
