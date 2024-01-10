
![Screen Recording 2024-01-10 at 10 10 09 AM 2](https://github.com/BenoitSchiermeier/MazeGame/assets/132936530/7577a7b3-1118-4edf-8d05-8301d83b842b)


![Screen Recording 2024-01-10 at 10 10 09 AM 2 (1)](https://github.com/BenoitSchiermeier/MazeGame/assets/132936530/1cbabce2-5b38-4a9d-9c3e-49d2058a8a60)


# MazeGame

# Overview
FloodItWorld is a Java-based puzzle game inspired by the classic Flood-It. The game's objective is to fill the entire board with a single color using a limited number of attempts. Players achieve this by flooding cells with colors, starting from the top-left cell.

# Features
Dynamic Board Generation: The board is randomly generated with a specified size and number of colors.

Configurable Difficulty: Players can set the board size and the number of colors, which affects the game's difficulty.

Color Flooding Mechanism: Clicking a non-flooded cell changes the color of the flooded area to that cell's color, expanding the flooded area.

Attempt Limitation: The game imposes a maximum number of attempts, adding a strategic challenge.

Reset Functionality: Players can reset the game at any point.

End Game Scenarios: The game ends when either the entire board is flooded with one color or the player runs out of attempts.


# Classes and Interfaces
Main Classes
FloodItWorld: Extends World, representing the game's main logic and display.
Cell: Represents individual cells on the board with their own color and flood status.
Interfaces
ICell: An interface defining behaviors of a cell, such as setting adjacent cells and determining if flooding should occur.
Utility Classes
MtCell: Implements ICell, representing an empty cell with no color or neighbors.
# Gameplay
Initialization: The game starts with a randomly generated board of colored cells.
Player Interaction: Players click on cells to flood neighboring cells of the same color.
Progression: The flooded area grows as players click on adjacent cells of different colors.
End Game: The game ends when either all cells are flooded with one color or the player exceeds the allowed number of attempts.
# Methods
Key Methods
makeScene(): Renders the current state of the game.
onTick(): Updates the game state, including the flood progression.
onMousePressed(Posn pos): Processes mouse clicks, changing the color of the flooded area.
onKeyEvent(String key): Handles key events like resetting the game.
# Test Class
ExamplesFloodIt: Contains tests for various aspects of the game, including board generation, gameplay mechanics, and user interactions.
Running the Game
To play FloodItWorld, compile and run the FloodItWorld class. Ensure all dependencies are properly set up in your Java project.

